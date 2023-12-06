package org.nsu.snake.client;

import org.nsu.snake.client.net.ClientSocket;
import org.nsu.snake.client.view.ClientGUI;
import org.nsu.snake.model.GameConfig;
import org.nsu.snake.model.ModelMain;
import org.nsu.snake.model.components.GamePlayer;
import org.nsu.snake.model.components.NodeRole;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

import java.io.IOException;
import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

public class ClientMain extends Thread {
    private ClientGUI clientGUI;
    private ModelMain modelMain;
    private ClientSocket clientSocket;
    private GamePlayer gamePlayer;
    private int delay;
    public ClientMain() {
        clientGUI = new ClientGUI(this);
    }

    public void startNewGame(GameConfig gameConfig, GamePlayer gamePlayer, String gameName) {
        delay = gameConfig.getStateDelayMs();
        this.gamePlayer = gamePlayer;
        modelMain = new ModelMain(gameConfig, gamePlayer, NodeRole.MASTER, gameName);
        try {
            clientSocket = new ClientSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.start();
    }
    @Override
    public void run() {
        startGameRoutine();
        startAnnouncement();
        startListening();
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
                    if (modelMain.getNodeRole(gamePlayer).equals(NodeRole.MASTER)) {
                        modelMain.calculateNextState(clientGUI.getCurrentDirection(), gamePlayer);
                        clientSocket.sendMessage(modelMain.getGameState());
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
                    if (modelMain.getNodeRole(gamePlayer).equals(NodeRole.MASTER)) {
                        clientSocket.sendMessage(modelMain.getGameAnnouncement());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 100);
    }

    private void startListening() {
        while (! Thread.currentThread().isInterrupted()) {
            try {
                ClientSocket.SocketAnswer answer = clientSocket.getMessage();
                if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.STATE)) {
                    clientGUI.repaintField(answer.gameMessage.getState().getState());
                }
                else if (answer.gameMessage.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT)) {
                    String ip = answer.senderIP;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void main(String[] args) {
        ClientMain clientMain = new ClientMain();
    }
}
