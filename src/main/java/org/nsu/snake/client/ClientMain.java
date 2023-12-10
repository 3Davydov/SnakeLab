package org.nsu.snake.client;

import org.nsu.snake.client.management.*;
import org.nsu.snake.client.net.ClientSocket;
import org.nsu.snake.client.view.ClientGUI;
import org.nsu.snake.model.GameConfig;
import org.nsu.snake.model.ModelMain;
import org.nsu.snake.model.components.Direction;
import org.nsu.snake.model.components.GameInfo;
import org.nsu.snake.model.components.GamePlayer;
import org.nsu.snake.model.components.NodeRole;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

import java.io.IOException;
import java.util.*;

public class ClientMain {
    private final ClientGUI clientGUI;
    private ModelMain modelMain = null; // TODO обнулять, когда перестаем быть хостом
    public final ClientSocket clientSocket;
    private GamePlayer gamePlayer;
    private int delay;
    private final Map<String,  NearbyGame> gamesAroundMap; // TODO удалять ip хоста из списка после конца игры
    private final Map<String, ChronologyPlayer> gamePlayerMap; // TODO удалять игрока, когда он выходит
    private boolean isGamePlayer = false; // TODO не забываем менять состояние
    private long knownStateOrder = -1; // TODO занулять после окончания игры
    private long selfStateOrder = 0; // TODO занулять после окончания игры
    private int masterId = -1; // TODO занулять после окончания игры
    private MessageManager messageManager = null; // TODO останавливать и удалять после конца игры
    private PingManager pingManager = null;
    private RoleManager roleManager;

    private SnakesProto.GameMessage lastStateMessage = null;
    public ClientMain() {
        gamesAroundMap = new HashMap<>();
        gamePlayerMap = new HashMap<>();
        try {
            clientSocket = new ClientSocket(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clientGUI = new ClientGUI(this);
        roleManager = new RoleManager(this);
    }
    public void startNewGame(GameConfig gameConfig, GamePlayer gamePlayer, String gameName) {
        delay = gameConfig.getStateDelayMs();
        this.gamePlayer = gamePlayer;
        this.gamePlayer.setIP(clientSocket.getSelfIP());
        this.gamePlayer.setPort(clientSocket.getSelfPort());
        this.gamePlayer.setNodeRole(NodeRole.MASTER);
        gamePlayerMap.put(clientSocket.getSelfIP(), new ChronologyPlayer(this.gamePlayer));

        modelMain = new ModelMain(gameConfig, gamePlayer, NodeRole.MASTER, gameName, this);

        messageManager = new MessageManager(gameConfig.getStateDelayMs() / 10, this);
        pingManager = new PingManager(this, gameConfig.getStateDelayMs() / 10);
        startGameRoutine();
        startAnnouncement();
        isGamePlayer = true;
    }
    public void joinGame(GameInfo gameInfo, String playerName, String playerType) {
        try {
            SnakesProto.GameMessage joinMessage = createJoinMsg(gameInfo, playerName, playerType);
            clientSocket.sendUnicastMessage(gameInfo.getLeaderIP(), gameInfo.getPort(), joinMessage);

            messageManager = new MessageManager(100, this); // TODO по хорошему передавать тут delay игры
            messageManager.addMessageToConfirmList(new UnconfirmedMessageInfo(gameInfo.getPort(), gameInfo.getLeaderIP(), joinMessage.getMsgSeq()), joinMessage);

            pingManager = new PingManager(this, 100); // TODO тоже передавать delay игры
            pingManager.addTrackedNode(new TrackedNode(gameInfo.getPort(), gameInfo.getLeaderIP()));
            pingManager.rebootTrackedNode_send(new TrackedNode(gameInfo.getPort(), gameInfo.getLeaderIP()));

            this.gamePlayer = new GamePlayer(playerName);
            this.gamePlayer.setIP(clientSocket.getSelfIP());
            this.gamePlayer.setPort(clientSocket.getSelfPort());
            this.gamePlayer.setHost(gameInfo.getLeaderIP(), gameInfo.getPort());

            switch (playerType) {
                case "NORMAL" -> this.gamePlayer.setNodeRole(NodeRole.NORMAL);
                case "VIEWER" -> this.gamePlayer.setNodeRole(NodeRole.VIEWER);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public SnakesProto.GameMessage createJoinMsg(GameInfo gameInfo, String playerName, String playerType) {
        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        gameMessageBuilder.setMsgSeq(1);

        SnakesProto.GameMessage.JoinMsg.Builder joinMessageBuilder = SnakesProto.GameMessage.JoinMsg.newBuilder();
        joinMessageBuilder.setPlayerType(SnakesProto.PlayerType.HUMAN);
        joinMessageBuilder.setPlayerName(playerName);
        joinMessageBuilder.setGameName(gameInfo.getLeader());

        switch (playerType) {
            case "NORMAL" -> joinMessageBuilder.setRequestedRole(SnakesProto.NodeRole.NORMAL);
            case "VIEWER" -> joinMessageBuilder.setRequestedRole(SnakesProto.NodeRole.VIEWER);
            default -> System.out.println("UNKNOWN ROLE");
        }

        gameMessageBuilder.setJoin(joinMessageBuilder);

        return gameMessageBuilder.build();
    }
    private void startGameRoutine() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (Thread.currentThread().isInterrupted()) {
                    timer.cancel();
                    return;
                }
                try {
                    if (gamePlayer.getNodeRole().equals(NodeRole.MASTER)) {
                        modelMain.setDirection(gamePlayer, clientGUI.getCurrentDirection());
                        modelMain.calculateNextState();

                        ArrayList<GamePlayer> playersList = modelMain.getAllPlayers();
                        SnakesProto.GameMessage messageToSend = modelMain.getGameState();
                        for (GamePlayer player : playersList) {
                            if (! player.equals(gamePlayer)) {
                                clientSocket.sendUnicastMessage(player.getIpAddress(), player.getPort(), messageToSend);
                                messageManager.addMessageToConfirmList(
                                        new UnconfirmedMessageInfo(player.getPort(), player.getIpAddress(), messageToSend.getMsgSeq()),
                                        messageToSend);
                                pingManager.rebootTrackedNode_send(new TrackedNode(player.getPort(), player.getIpAddress()));
                            }
                            else
                                clientGUI.repaintField(messageToSend.getState().getState());
                        }
                        modelMain.incrementStateOrder();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, delay);
    }
    private void startAnnouncement() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (Thread.currentThread().isInterrupted()) {
                    timer.cancel();
                    return;
                }
                try {
                    if (gamePlayer.getNodeRole().equals(NodeRole.MASTER)) {
                        clientSocket.sendMessage(modelMain.getGameAnnouncement());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 1000);
    }
    public void processIncomingMessage(ClientSocket.SocketAnswer answer) {
        if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.STATE) && isGamePlayer) {
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));
            if (knownStateOrder < answer.gameMessage.getMsgSeq()) {
                clientGUI.repaintField(answer.gameMessage.getState().getState());
                knownStateOrder = answer.gameMessage.getMsgSeq();
                roleManager.refreshPlayerRoles(answer.gameMessage.getState().getState().getPlayers());
                lastStateMessage = answer.gameMessage;
                try {
                    sendAckMessage(masterId, answer.senderPort, answer.senderIP, answer.gameMessage.getMsgSeq());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                System.out.println("GOT OLD MESSAGE. CURRENT STATE ORDER :" + " " + knownStateOrder + " MESSAGE SEQ :" + " " + answer.gameMessage.getMsgSeq());
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT)) {
            String ip = answer.senderIP;
            int port = answer.senderPort;
            gamesAroundMap.put(ip, new NearbyGame(answer.gameMessage.getAnnouncement(), port, ip,
                                new GameConfig(answer.gameMessage.getAnnouncement().getGames(0).getConfig().getWidth(),
                                                answer.gameMessage.getAnnouncement().getGames(0).getConfig().getHeight(),
                                                answer.gameMessage.getAnnouncement().getGames(0).getConfig().getFoodStatic(),
                                                answer.gameMessage.getAnnouncement().getGames(0).getConfig().getStateDelayMs()),
                                answer.gameMessage.getAnnouncement().getGames(0).getGameName()));
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.ACK)) {
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));
            messageManager.removeMessageFromConfirmList(new UnconfirmedMessageInfo(answer.senderPort, answer.senderIP, answer.gameMessage.getMsgSeq()));
            if (masterId == -1) masterId = answer.gameMessage.getSenderId();
//            if (knownStateOrder >= answer.gameMessage.getMsgSeq()) return;
            if (clientGUI.boardView == null) { // TODO делать null в конце игры
                SnakesProto.GameConfig gameConfig = gamesAroundMap.get(answer.senderIP).gameAnnouncement.getGames(0).getConfig();
                clientGUI.paintFieldAtFirst(new GameConfig(gameConfig.getWidth(), gameConfig.getHeight(), gameConfig.getFoodStatic(), gameConfig.getStateDelayMs()));
                this.gamePlayer.setID(answer.gameMessage.getReceiverId());
                this.delay = gamesAroundMap.get(this.gamePlayer.getHostIP()).gameConfig.getStateDelayMs();
                isGamePlayer = true;
                startGameRoutine();
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.STEER)) {
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));
            if (gamePlayerMap.get(answer.senderIP) == null) {
                System.out.println("GOT STEER MESSAGE FROM UNKNOWN PLAYER");
                return;
            }
            if (answer.gameMessage.getMsgSeq() < gamePlayerMap.get(answer.senderIP).seqNumber) return;
            else gamePlayerMap.get(answer.senderIP).seqNumber = answer.gameMessage.getMsgSeq();

            switch (answer.gameMessage.getSteer().getDirection()) {
                case UP -> gamePlayerMap.get(answer.senderIP).player.setDirection(Direction.UP);
                case DOWN -> gamePlayerMap.get(answer.senderIP).player.setDirection(Direction.DOWN);
                case LEFT -> gamePlayerMap.get(answer.senderIP).player.setDirection(Direction.LEFT);
                case RIGHT -> gamePlayerMap.get(answer.senderIP).player.setDirection(Direction.RIGHT);
            }

            modelMain.setDirection(gamePlayerMap.get(answer.senderIP).player, gamePlayerMap.get(answer.senderIP).player.getDirection());
            try {
                sendAckMessage(gamePlayerMap.get(answer.senderIP).player.getId(), answer.senderPort, answer.senderIP, answer.gameMessage.getMsgSeq());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.JOIN)) {
            System.out.println("JOIN REQUEST");
            SnakesProto.GameMessage.JoinMsg joinMsg = answer.gameMessage.getJoin();
            GamePlayer gamePlayer = new GamePlayer(joinMsg.getPlayerName());
            gamePlayer.setIP(answer.senderIP);
            gamePlayer.setPort(answer.senderPort);

            NodeRole role = NodeRole.NORMAL;
            switch (joinMsg.getRequestedRole()) {
                case MASTER -> role = NodeRole.MASTER;
                case DEPUTY -> role = NodeRole.DEPUTY;
                case NORMAL -> role = NodeRole.NORMAL;
                case VIEWER -> role = NodeRole.VIEWER;
            }
            if (role.equals(NodeRole.MASTER) || role.equals(NodeRole.VIEWER)) {
                // TODO обработать отдельно
            }
            if (role.equals(NodeRole.NORMAL) && modelMain.getAllPlayers().size() == 1) role = NodeRole.DEPUTY;
            roleManager.addPlayer(gamePlayer, role);

            int id = modelMain.addNewPlayer(gamePlayer, role);
            if (id == -1) return;

            ChronologyPlayer chronologyPlayer = new ChronologyPlayer(gamePlayer);

            gamePlayerMap.put(answer.senderIP, chronologyPlayer);
            try {
                sendAckMessage(id, gamePlayer.getPort(), gamePlayer.getIpAddress(), answer.gameMessage.getMsgSeq());
                pingManager.addTrackedNode(new TrackedNode(gamePlayer.getPort(), gamePlayer.getIpAddress()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.PING)) {
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));
            if (modelMain != null) {
                try {
                    sendAckMessage(gamePlayerMap.get(answer.senderIP).player.getId(), answer.senderPort, answer.senderIP, answer.gameMessage.getMsgSeq());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                try {
                    sendAckMessage(0, gamePlayer.getHostPort(), gamePlayer.getHostIP(), answer.gameMessage.getMsgSeq());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    private void sendAckMessage(int receiverID, int receiverPort, String receiverIP, long seq) throws IOException {
        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        gameMessageBuilder.setSenderId(gamePlayer.getId());
        gameMessageBuilder.setReceiverId(receiverID);
        gameMessageBuilder.setMsgSeq(seq);

        SnakesProto.GameMessage.AckMsg.Builder ackMessageBuilder = SnakesProto.GameMessage.AckMsg.newBuilder();
        gameMessageBuilder.setAck(ackMessageBuilder);

        clientSocket.sendUnicastMessage(receiverIP, receiverPort, gameMessageBuilder.build());
        pingManager.rebootTrackedNode_send(new TrackedNode(receiverPort, receiverIP));
    }
    public void sendSteerMessage() throws IOException {
        Direction direction = clientGUI.getCurrentDirection();

        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        gameMessageBuilder.setMsgSeq(selfStateOrder);

        SnakesProto.GameMessage.SteerMsg.Builder steerMessageBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
        switch (direction) {
            case UP -> steerMessageBuilder.setDirection(SnakesProto.Direction.UP);
            case DOWN -> steerMessageBuilder.setDirection(SnakesProto.Direction.DOWN);
            case LEFT -> steerMessageBuilder.setDirection(SnakesProto.Direction.LEFT);
            case RIGHT -> steerMessageBuilder.setDirection(SnakesProto.Direction.RIGHT);
        }

        gameMessageBuilder.setSteer(steerMessageBuilder);
        clientSocket.sendUnicastMessage(this.gamePlayer.getHostIP(), this.gamePlayer.getHostPort(), gameMessageBuilder.build());
        pingManager.rebootTrackedNode_send(new TrackedNode(this.gamePlayer.getHostPort(), this.gamePlayer.getHostIP()));
        messageManager.addMessageToConfirmList(
                new UnconfirmedMessageInfo(this.gamePlayer.getHostPort(), this.gamePlayer.getHostIP(), selfStateOrder),
                gameMessageBuilder.build());
        selfStateOrder += 1;
    }
    public NodeRole getNodeRole() {return this.gamePlayer.getNodeRole();}
    private GameInfo parseAnnouncementMessage(SnakesProto.GameAnnouncement message, String ip, int port) {
        List<SnakesProto.GamePlayer> arr = message.getPlayers().getPlayersList();
        int playersNum = arr.size();
        SnakesProto.GameConfig conf = message.getConfig();
        return new GameInfo(message.getGameName(), ip, conf.getWidth(), conf.getHeight(), playersNum, conf.getFoodStatic(), port);
    }
    public ArrayList<GameInfo> getGamesAround() {
        ArrayList<GameInfo> ret = new ArrayList<>();
        ArrayList<NearbyGame> iter = new ArrayList<>(gamesAroundMap.values());

        for (int i = 0; i < iter.size(); i++) {
            ret.add(parseAnnouncementMessage(iter.get(i).gameAnnouncement.getGames(0), iter.get(i).nearbyIp, iter.get(i).nearbyPort));
        }
        return ret;
    }
    public void sendErrorMessage(String message, GamePlayer player) throws IOException {
        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        gameMessageBuilder.setMsgSeq(selfStateOrder);

        SnakesProto.GameMessage.ErrorMsg.Builder errorMessageBuilder = SnakesProto.GameMessage.ErrorMsg.newBuilder();
        errorMessageBuilder.setErrorMessage(message);

        gameMessageBuilder.setError(errorMessageBuilder);
        clientSocket.sendUnicastMessage(player.getIpAddress(), player.getPort(), gameMessageBuilder.build());
    }
    public void sendPingMessage(int destPort, String destIP) throws IOException {
        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        if (modelMain != null) {
            gameMessageBuilder.setMsgSeq(modelMain.getStateOrder());
            modelMain.incrementStateOrder();
        }
        else {
            gameMessageBuilder.setMsgSeq(selfStateOrder);
            selfStateOrder += 1;
        }

        SnakesProto.GameMessage.PingMsg.Builder pingMessageBuilder = SnakesProto.GameMessage.PingMsg.newBuilder();
        gameMessageBuilder.setPing(pingMessageBuilder);

        clientSocket.sendUnicastMessage(destIP, destPort, gameMessageBuilder.build());
//        pingManager.rebootTrackedNode(new TrackedNode(destPort, destIP)); TODO без reboot потому что это уже есть внутри класса PingManager
        messageManager.addMessageToConfirmList(new UnconfirmedMessageInfo(destPort, destIP, gameMessageBuilder.getMsgSeq()), gameMessageBuilder.build());
    }
    public void processNodeDeath(TrackedNode node) {
        NodeRole deadNodeRole = roleManager.getPlayerRole(node);
        System.out.println("NODE WITH ROLE " + deadNodeRole + " DEAD");
        String deadHostIP = roleManager.getNodeWithRole(NodeRole.MASTER).ip;
        GameConfig currentGameConfig = gamesAroundMap.get(deadHostIP).gameConfig;
        String gameName = gamesAroundMap.get(deadHostIP).gameName;
        modelMain = new ModelMain(currentGameConfig, gameName, this, lastStateMessage);
    }
    public class NearbyGame {
        public NearbyGame(SnakesProto.GameMessage.AnnouncementMsg newAnnouncement, int newPort, String newIP, GameConfig newConfig, String gameName) {
            nearbyIp = newIP;
            nearbyPort = newPort;
            gameAnnouncement = newAnnouncement;
            this.gameConfig = newConfig;
            this.gameName = gameName;
        }
        public SnakesProto.GameMessage.AnnouncementMsg gameAnnouncement;
        public String nearbyIp;
        public int nearbyPort;
        public GameConfig gameConfig;

        public String gameName;
    }
    public class ChronologyPlayer {
        public GamePlayer player;
        public long seqNumber;
        public ChronologyPlayer(GamePlayer newPlayer) {
            this.player = newPlayer;
            seqNumber = -1;
        }
    }
    public static void main(String[] args) {
        ClientMain clientMain = new ClientMain();
    }
}
// TODO game name должно быть глобально уникальным