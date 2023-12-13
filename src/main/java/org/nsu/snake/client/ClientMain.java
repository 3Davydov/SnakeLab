package org.nsu.snake.client;

import org.nsu.snake.client.management.*;
import org.nsu.snake.client.net.ClientSocket;
import org.nsu.snake.client.view.ClientGUI;
import org.nsu.snake.model.GameConfig;
import org.nsu.snake.model.ModelMain;
import org.nsu.snake.model.components.*;
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
    private final Map<TrackedNode, ChronologyPlayer> gamePlayerMap; // TODO удалять игрока, когда он выходит
    private boolean isGamePlayer = false; // TODO не забываем менять состояние
    private long knownStateOrder = -1; // TODO занулять после окончания игры
    private long selfStateOrder = 0; // TODO занулять после окончания игры
    private int masterId = -1; // TODO занулять после окончания игры
    private MessageManager messageManager = null; // TODO останавливать и удалять после конца игры
    private PingManager pingManager = null;
    private final RoleManager roleManager;
    private SnakesProto.GameMessage lastStateMessage = null; // TODO занулять после выхода из игры
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
    public boolean gameNameIsUnique(String name) {
        ArrayList<NearbyGame> nearbyGamesNames = new ArrayList<>(gamesAroundMap.values());
        for (NearbyGame g : nearbyGamesNames) {
            if (g.gameName.equals(name)) return false;
        }
        return true;
    }
    public void startNewGame(GameConfig gameConfig, GamePlayer gamePlayer, String gameName) {
        delay = gameConfig.getStateDelayMs();
        this.gamePlayer = gamePlayer;
        this.gamePlayer.setIP(clientSocket.getSelfIP());
        this.gamePlayer.setPort(clientSocket.getSelfPort());
        this.gamePlayer.setNodeRole(NodeRole.MASTER);
        gamePlayerMap.put(new TrackedNode(clientSocket.getSelfPort(), clientSocket.getSelfIP()), new ChronologyPlayer(this.gamePlayer));

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
            pingManager = new PingManager(this, 100); // TODO тоже передавать delay игры
            clientSocket.sendUnicastMessage(gameInfo.getLeaderIP(), gameInfo.getPort(), joinMessage);

            messageManager = new MessageManager(100, this); // TODO по хорошему передавать тут delay игры
            messageManager.addMessageToConfirmList(new UnconfirmedMessageInfo(gameInfo.getPort(), gameInfo.getLeaderIP(), joinMessage.getMsgSeq()), joinMessage);

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
                                clientGUI.repaintField(messageToSend.getState().getState(), getPlayersStatistic());
                        }

                        ArrayList<GamePlayer> viewersList = modelMain.getAllViewers();
                        for (GamePlayer player : viewersList) {
                            clientSocket.sendUnicastMessage(player.getIpAddress(), player.getPort(), messageToSend);
                            messageManager.addMessageToConfirmList(
                                    new UnconfirmedMessageInfo(player.getPort(), player.getIpAddress(), messageToSend.getMsgSeq()),
                                    messageToSend);
                            pingManager.rebootTrackedNode_send(new TrackedNode(player.getPort(), player.getIpAddress()));
                        }
                        modelMain.incrementStateOrder();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, delay);
    }
    private ArrayList<PlayerStatistic> getPlayersStatistic() {
        ArrayList<PlayerStatistic> stats = new ArrayList<>();
        ArrayList<GamePlayer> playersList = modelMain.getAllPlayers();
        for (int i = 0; i < playersList.size(); i++) {
            PlayerStatistic ps = new PlayerStatistic(playersList.get(i).getName(), playersList.get(i).getScore(), playersList.get(i).getNodeRole());
            stats.add(ps);
        }
        return stats;
    }
    private ArrayList<PlayerStatistic> getPlayersStatistic(ArrayList<SnakesProto.GamePlayer> players) {
        ArrayList<PlayerStatistic> stats = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            NodeRole role = NodeRole.NORMAL;
            switch (players.get(i).getRole()) {
                case MASTER -> role = NodeRole.MASTER;
                case DEPUTY -> role = NodeRole.DEPUTY;
                case NORMAL -> role = NodeRole.NORMAL;
                case VIEWER -> role = NodeRole.VIEWER;
            }

            PlayerStatistic playerStatistic = new PlayerStatistic(players.get(i).getName(), players.get(i).getScore(), role);
            stats.add(playerStatistic);
        }
        return stats;
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
            if (this.gamePlayer.getNodeRole().equals(NodeRole.MASTER) ||
                    this.gamePlayer.getHostPort() != answer.senderPort || (! Objects.equals(this.gamePlayer.getHostIP(), answer.senderIP))) {
                System.out.println("GOT STATE MESSAGE FROM UNKNOWN MASTER");
                return;
            }
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));
            if (knownStateOrder < answer.gameMessage.getMsgSeq()) {
                ArrayList<SnakesProto.GamePlayer> srcPlayers = new ArrayList<>(answer.gameMessage.getState().getState().getPlayers().getPlayersList());
                clientGUI.repaintField(answer.gameMessage.getState().getState(), getPlayersStatistic(srcPlayers));
                knownStateOrder = answer.gameMessage.getMsgSeq();
                roleManager.refreshPlayerRoles(answer.gameMessage.getState().getState().getPlayers());
                this.gamePlayer.setNodeRole(roleManager.getPlayerRole(this.gamePlayer));
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
            if (this.gamePlayer.getId() == -1) this.gamePlayer.setID(answer.gameMessage.getReceiverId());
//            if (knownStateOrder >= answer.gameMessage.getMsgSeq()) return;
            if (clientGUI.boardView == null) { // TODO делать null в конце игры
                SnakesProto.GameConfig gameConfig = gamesAroundMap.get(answer.senderIP).gameAnnouncement.getGames(0).getConfig();
                PlayerStatistic selfPlayerStatistic = new PlayerStatistic(this.gamePlayer.getName(), 0, this.gamePlayer.getNodeRole());
                PlayerStatistic hostPlayerStatistic = new PlayerStatistic(" ", 0, NodeRole.MASTER);
                ArrayList<PlayerStatistic> data = new ArrayList<>();
                data.add(selfPlayerStatistic);
                data.add(hostPlayerStatistic);
                clientGUI.paintFieldAtFirst(new GameConfig(gameConfig.getWidth(), gameConfig.getHeight(), gameConfig.getFoodStatic(), gameConfig.getStateDelayMs()), data);
//                this.gamePlayer.setID(answer.gameMessage.getReceiverId());
                this.delay = gamesAroundMap.get(this.gamePlayer.getHostIP()).gameConfig.getStateDelayMs();
                isGamePlayer = true;
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.STEER)) {
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));
            if (gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)) == null) {
                System.out.println("GOT STEER MESSAGE FROM UNKNOWN PLAYER");
                return;
            }
//            System.out.println(answer.senderIP + " " + answer.senderPort + " " +
//                    gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player.getId() + " " + gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)));
            if (answer.gameMessage.getMsgSeq() < gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).seqNumber) return;
            else gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).seqNumber = answer.gameMessage.getMsgSeq();

            switch (answer.gameMessage.getSteer().getDirection()) {
                case UP -> gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player.setDirection(Direction.UP);
                case DOWN -> gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player.setDirection(Direction.DOWN);
                case LEFT -> gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player.setDirection(Direction.LEFT);
                case RIGHT -> gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player.setDirection(Direction.RIGHT);
            }

            modelMain.setDirection(gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player,
                                    gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player.getDirection());
            try {
                sendAckMessage(gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player.getId(),
                                answer.senderPort, answer.senderIP, answer.gameMessage.getMsgSeq());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.JOIN)) {
            System.out.println("JOIN REQUEST");
            SnakesProto.GameMessage.JoinMsg joinMsg = answer.gameMessage.getJoin();
            if (!modelMain.gamePlayerNameIsUnique(joinMsg.getPlayerName())) {
                sendJoinRefuse("Player with this name already exists", answer.senderIP, answer.senderPort);
                return;
            }
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
            if (role.equals(NodeRole.MASTER)) {
                System.out.println("SOMEBODY TRUING TO CONNECT GAME WITH ROLE MASTER");
                sendJoinRefuse("You cannot join to existing game as MASTER", answer.senderIP, answer.senderPort);
                return;
            }
            else if (role.equals(NodeRole.NORMAL) && modelMain.getAllPlayers().size() == 1) {
                role = NodeRole.DEPUTY;
            }
            roleManager.addPlayer(gamePlayer, role);

            int id = modelMain.addNewPlayer(gamePlayer, role);
            if (id == -1) return;
            gamePlayer.setID(id);

            ChronologyPlayer chronologyPlayer = new ChronologyPlayer(gamePlayer);

            gamePlayerMap.put(new TrackedNode(answer.senderPort, answer.senderIP), chronologyPlayer);
            try {
                sendAckMessage(id, gamePlayer.getPort(), gamePlayer.getIpAddress(), answer.gameMessage.getMsgSeq());
                pingManager.addTrackedNode(new TrackedNode(gamePlayer.getPort(), gamePlayer.getIpAddress()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.PING)) {
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));

            if (this.gamePlayer.getNodeRole().equals(NodeRole.MASTER)) {
                try {
                    sendAckMessage(gamePlayerMap.get(new TrackedNode(answer.senderPort, answer.senderIP)).player.getId(),
                                    answer.senderPort, answer.senderIP, answer.gameMessage.getMsgSeq());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                try {
                    sendAckMessage(masterId, gamePlayer.getHostPort(), gamePlayer.getHostIP(), answer.gameMessage.getMsgSeq());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.ERROR)) {
            System.out.println("GOT ERROR MESSAGE");
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));
            try {
                sendAckMessage(answer.gameMessage.getSenderId(), answer.senderPort, answer.senderIP, answer.gameMessage.getMsgSeq());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (lastStateMessage == null) { // Значит, что не смогли подключиться
                clientGUI.displayError(answer.gameMessage.getError().getErrorMessage());
            }
        }
        else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.ROLE_CHANGE)) {
            pingManager.rebootTrackedNode_receive(new TrackedNode(answer.senderPort, answer.senderIP));
            NodeRole senderRole = NodeRole.MASTER;
            NodeRole receiverRole = NodeRole.NORMAL;
            int senderID = answer.gameMessage.getSenderId();
            int receiverID = answer.gameMessage.getReceiverId();
            if (receiverID != this.gamePlayer.getId()) {
                System.out.println("ROLE CHANGE MESSAGE SENT TO WRONG NODE");
                return;
            }
            switch (answer.gameMessage.getRoleChange().getSenderRole()) {
                case MASTER -> senderRole = NodeRole.MASTER;
                case DEPUTY -> senderRole = NodeRole.DEPUTY;
                case NORMAL -> senderRole = NodeRole.NORMAL;
                case VIEWER -> senderRole = NodeRole.VIEWER;
            }
            switch (answer.gameMessage.getRoleChange().getReceiverRole()) {
                case MASTER -> receiverRole = NodeRole.MASTER;
                case DEPUTY -> receiverRole = NodeRole.DEPUTY;
                case NORMAL -> receiverRole = NodeRole.NORMAL;
                case VIEWER -> receiverRole = NodeRole.VIEWER;
            }

            pingManager.removeTrackedNode(new TrackedNode(this.gamePlayer.getPort(), this.gamePlayer.getHostIP()));
            this.gamePlayer.setHost(answer.senderIP, answer.senderPort);

            if (senderRole.equals(NodeRole.MASTER)) {
                TrackedNode oldMaster = roleManager.getPlayerWithRole(NodeRole.MASTER);
                if (oldMaster.port != this.gamePlayer.getHostPort() || (! Objects.equals(oldMaster.ip, this.gamePlayer.getHostIP()))) {
                    System.out.println("GOT NEW MASTER");
                    roleManager.removePlayer(oldMaster);
                    roleManager.addPlayer(new TrackedNode(answer.senderPort, answer.senderIP), NodeRole.MASTER);
                    this.gamePlayer.setHost(answer.senderIP, answer.senderPort);
                }
            }
            if (receiverRole.equals(NodeRole.DEPUTY)) {
                System.out.println("NOW I AM DEPUTY");
                TrackedNode oldDeputy = roleManager.getPlayerWithRole(NodeRole.DEPUTY);
                if (oldDeputy != null) roleManager.removePlayer(oldDeputy);
            }
            this.gamePlayer.setNodeRole(receiverRole);
            try {
                sendAckMessage(senderID, answer.senderPort, answer.senderIP, answer.gameMessage.getMsgSeq());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void sendJoinRefuse(String reason, String receiverIP, int receiverPort) {
        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        gameMessageBuilder.setMsgSeq(selfStateOrder);

        SnakesProto.GameMessage.ErrorMsg.Builder errorMessageBuilder = SnakesProto.GameMessage.ErrorMsg.newBuilder();
        errorMessageBuilder.setErrorMessage(reason);

        gameMessageBuilder.setError(errorMessageBuilder);
        try {
            clientSocket.sendUnicastMessage(receiverIP, receiverPort, gameMessageBuilder.build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return;
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
//        System.out.println("SENT STEER TO " + this.gamePlayer.getHostIP() + " " + this.gamePlayer.getHostPort());
        pingManager.rebootTrackedNode_send(new TrackedNode(this.gamePlayer.getHostPort(), this.gamePlayer.getHostIP()));
        messageManager.addMessageToConfirmList(
                new UnconfirmedMessageInfo(this.gamePlayer.getHostPort(), this.gamePlayer.getHostIP(), selfStateOrder),
                gameMessageBuilder.build()
        );
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
        pingManager.rebootTrackedNode_send(new TrackedNode(player.getPort(), player.getIpAddress()));
        messageManager.addMessageToConfirmList(
                new UnconfirmedMessageInfo(player.getPort(), player.getIpAddress(), selfStateOrder),
                gameMessageBuilder.build()
        );
    }
    public void sendPingMessage(int destPort, String destIP) throws IOException {
        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        if (this.gamePlayer.getNodeRole().equals(NodeRole.MASTER)) {
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
        messageManager.clearMessageToConfirmList();
        NodeRole deadNodeRole = roleManager.getPlayerRole(node);
        System.out.println("NODE WITH ROLE " + deadNodeRole + " DEAD");
        if (this.gamePlayer.getNodeRole().equals(NodeRole.DEPUTY) && deadNodeRole.equals(NodeRole.MASTER)) {
            String deadHostIP = roleManager.getPlayerWithRole(NodeRole.MASTER).ip;
            roleManager.removePlayer(node);
            roleManager.addPlayer(this.gamePlayer, NodeRole.MASTER);

            GameConfig currentGameConfig = gamesAroundMap.get(deadHostIP).gameConfig;
            String gameName = gamesAroundMap.get(deadHostIP).gameName;
            modelMain = new ModelMain(currentGameConfig, gameName, this, lastStateMessage);
            this.gamePlayer.setNodeRole(NodeRole.MASTER);
            startGameRoutine();
            startAnnouncement();

            ArrayList<GamePlayer> allPlayers = new ArrayList<>(modelMain.getAllPlayers());
            if (allPlayers.size() == 1) return; // TODO возвращать именно живых

            TrackedNode newDeputy = roleManager.getPlayerWithRole(NodeRole.NORMAL);
            for (int i = 0; i < allPlayers.size(); i++) {
                if (allPlayers.get(i).equals(gamePlayer)) {
                    continue;
                }
                else if (allPlayers.get(i).getPort() == newDeputy.port && Objects.equals(allPlayers.get(i).getIpAddress(), newDeputy.ip)) {
                    try {
                        sendRoleChangeMessage(NodeRole.MASTER, NodeRole.DEPUTY, allPlayers.get(i).getPort(), allPlayers.get(i).getIpAddress(),
                                                allPlayers.get(i).getId(), this.gamePlayer.getId());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    try {
                        sendRoleChangeMessage(NodeRole.MASTER, allPlayers.get(i).getNodeRole(), allPlayers.get(i).getPort(), allPlayers.get(i).getIpAddress(),
                                                allPlayers.get(i).getId(), this.gamePlayer.getId());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                gamePlayerMap.put(new TrackedNode(allPlayers.get(i).getPort(), allPlayers.get(i).getIpAddress()), new ChronologyPlayer(allPlayers.get(i)));
            }
        }
        else if (this.gamePlayer.getNodeRole().equals(NodeRole.NORMAL) && deadNodeRole.equals(NodeRole.MASTER)) {
            messageManager.clearMessageToConfirmList();
            TrackedNode deputy = roleManager.getPlayerWithRole(NodeRole.DEPUTY);
            this.gamePlayer.setHost(deputy.ip, deputy.port);
        }
        else if (this.gamePlayer.getNodeRole().equals(NodeRole.MASTER) && deadNodeRole.equals(NodeRole.DEPUTY)) {
            GamePlayer removingPlayer = gamePlayerMap.get(roleManager.getPlayerWithRole(NodeRole.DEPUTY)).player;
            modelMain.removePlayer(removingPlayer);
            gamePlayerMap.remove(roleManager.getPlayerWithRole(NodeRole.DEPUTY));

            ArrayList<GamePlayer> allPlayers = new ArrayList<>(modelMain.getAllPlayers());
            if (allPlayers.size() == 1) return; // TODO возвращать именно живых

            TrackedNode newDeputy = roleManager.getPlayerWithRole(NodeRole.NORMAL);
            if (newDeputy == null) return;
            for (int i = 0; i < allPlayers.size(); i++) {
                if (allPlayers.get(i).equals(gamePlayer)) {
                    continue;
                }
                else if (allPlayers.get(i).getPort() == newDeputy.port && Objects.equals(allPlayers.get(i).getIpAddress(), newDeputy.ip)) {
                    try {
                        sendRoleChangeMessage(NodeRole.MASTER, NodeRole.DEPUTY, allPlayers.get(i).getPort(), allPlayers.get(i).getIpAddress(),
                                allPlayers.get(i).getId(), this.gamePlayer.getId());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        else if (this.gamePlayer.getNodeRole().equals(NodeRole.MASTER) && deadNodeRole.equals(NodeRole.NORMAL)) {
            GamePlayer removingPlayer = gamePlayerMap.get(node).player;
            modelMain.removePlayer(removingPlayer);
            gamePlayerMap.remove(node);
        }
        else if (this.gamePlayer.getNodeRole().equals(NodeRole.MASTER) && deadNodeRole.equals(NodeRole.VIEWER)) {
            GamePlayer removingPlayer = gamePlayerMap.get(node).player;
            modelMain.removeViewer(removingPlayer);
            gamePlayerMap.remove(node);
        }
    }
    private void sendRoleChangeMessage(NodeRole senderRole, NodeRole receiverRole, int receiverPort, String receiverIP, int receiverId, int senderID) throws IOException {
        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        gameMessageBuilder.setMsgSeq(selfStateOrder);
        gameMessageBuilder.setReceiverId(receiverId);
        gameMessageBuilder.setSenderId(senderID);

        SnakesProto.GameMessage.RoleChangeMsg.Builder roleChangeMessageBuilder = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();

        switch (senderRole) {
            case MASTER -> roleChangeMessageBuilder.setSenderRole(SnakesProto.NodeRole.MASTER);
            case NORMAL -> roleChangeMessageBuilder.setSenderRole(SnakesProto.NodeRole.NORMAL);
            case VIEWER -> roleChangeMessageBuilder.setSenderRole(SnakesProto.NodeRole.VIEWER);
            case DEPUTY -> roleChangeMessageBuilder.setSenderRole(SnakesProto.NodeRole.DEPUTY);
        }
        switch (receiverRole) {
            case MASTER -> roleChangeMessageBuilder.setReceiverRole(SnakesProto.NodeRole.MASTER);
            case NORMAL -> roleChangeMessageBuilder.setReceiverRole(SnakesProto.NodeRole.NORMAL);
            case VIEWER -> roleChangeMessageBuilder.setReceiverRole(SnakesProto.NodeRole.VIEWER);
            case DEPUTY -> roleChangeMessageBuilder.setReceiverRole(SnakesProto.NodeRole.DEPUTY);
        }

        gameMessageBuilder.setRoleChange(roleChangeMessageBuilder);
        clientSocket.sendUnicastMessage(receiverIP, receiverPort, gameMessageBuilder.build());
        pingManager.rebootTrackedNode_send(new TrackedNode(receiverPort, receiverIP));
        messageManager.addMessageToConfirmList(
                new UnconfirmedMessageInfo(receiverPort, receiverIP, selfStateOrder),
                gameMessageBuilder.build()
        );
        selfStateOrder += 1;
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

    public void quitGame() {

    }
    public static void main(String[] args) {
        ClientMain clientMain = new ClientMain();
    }
}