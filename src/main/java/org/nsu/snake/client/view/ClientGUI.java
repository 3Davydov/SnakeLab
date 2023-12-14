package org.nsu.snake.client.view;

import org.nsu.snake.client.ClientMain;
import org.nsu.snake.client.view.UIcomponents.BoardView;
import org.nsu.snake.client.view.UIcomponents.GameStatistics;
import org.nsu.snake.client.view.UIcomponents.GamesListView;
import org.nsu.snake.client.view.UIcomponents.MainMenu;
import org.nsu.snake.model.GameConfig;
import org.nsu.snake.model.ModelMain;
import org.nsu.snake.model.components.Direction;
import org.nsu.snake.model.components.GameInfo;
import org.nsu.snake.model.components.NodeRole;
import org.nsu.snake.model.components.PlayerStatistic;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;

public class ClientGUI {
    public JFrame mainFrame;
    private JPanel mainWindow;
    public JPanel actionWindow;
    private JPanel choiseWindow;
    private MainMenu mainMenu;
    private GamesListView gamesListView;
    private GameStatistics gameStatistics = null;
    public BoardView boardView = null;
    public ClientMain clientMain;
    public KeyListener actionWindowListener;
    private Direction currentDirection = Direction.LEFT;
    private Direction staticDirection = Direction.LEFT;

    private JFrame getJFrame() {
        JFrame jframe = new JFrame() {};
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dm = toolkit.getScreenSize();

        jframe.setLocation(0, 0);
        jframe.setSize(dm.width, dm.height);
        jframe.setTitle("SNAKE");
        return  jframe;
    }
    public ClientGUI(ClientMain newClient) {
        clientMain = newClient;

        mainFrame = getJFrame();
        mainWindow = new JPanel();
        mainFrame.add(mainWindow);
        mainWindow.setLayout(new BorderLayout());

        actionWindow = new JPanel();
        choiseWindow = new JPanel();

        mainMenu = new MainMenu(mainFrame, this);
        mainMenu.showMenu();

        gamesListView = new GamesListView(this, mainFrame);

        actionWindow.setBackground(Color.GRAY);
        actionWindow.setLayout(new BorderLayout());

        choiseWindow.setBackground(Color.YELLOW);
        choiseWindow.setLayout(new BorderLayout());
        choiseWindow.add(mainMenu.getMenu(), BorderLayout.CENTER);

        mainWindow.add(actionWindow, BorderLayout.CENTER);
        mainWindow.add(choiseWindow, BorderLayout.WEST);

        mainWindow.revalidate();
        mainFrame.revalidate();
        actionWindow.revalidate();
        choiseWindow.revalidate();
        mainFrame.setVisible(true);

        actionWindowListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == 'a') {
                    if (staticDirection != Direction.RIGHT)
                        currentDirection = Direction.LEFT;
                }
                else if (e.getKeyChar() == 's') {
                    if (staticDirection != Direction.UP)
                        currentDirection = Direction.DOWN;
                }
                else if (e.getKeyChar() == 'd') {
                    if (staticDirection != Direction.LEFT)
                        currentDirection = Direction.RIGHT;
                }
                else if (e.getKeyChar() == 'w') {
                    if (staticDirection != Direction.DOWN)
                        currentDirection = Direction.UP;
                }
                if (clientMain.getNodeRole().equals(NodeRole.NORMAL) || clientMain.getNodeRole().equals(NodeRole.DEPUTY)) {
                    try {
                        clientMain.sendSteerMessage();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        };
    }
    public void repaintField(SnakesProto.GameState gameState, ArrayList<PlayerStatistic> data) {
        boardView.repaintField(gameState);
        gameStatistics.printTable(data);

        gameStatistics.getTable().revalidate();
        gameStatistics.getTable().repaint();

        choiseWindow.revalidate();
        choiseWindow.repaint();

        actionWindow.repaint();
    }
    public void showGamesList(ArrayList<GameInfo> data) {
        actionWindow.removeAll();
        gamesListView.getTable().setSize(actionWindow.getWidth(), actionWindow.getHeight());
        actionWindow.add(gamesListView.getTable(), BorderLayout.CENTER);
        gamesListView.printTable(data);
        actionWindow.revalidate();
        actionWindow.repaint();

        mainWindow.revalidate();
        mainWindow.repaint();
    }
    public Direction getCurrentDirection() {
        staticDirection = currentDirection;
        return this.staticDirection;
    }
    public void returnToPrevView() {
        actionWindow.removeAll();
        actionWindow.setBackground(Color.GRAY);
        actionWindow.revalidate();
        actionWindow.repaint();

        choiseWindow.removeAll();
        choiseWindow.add(mainMenu.getMenu(), BorderLayout.CENTER);
        choiseWindow.revalidate();
        choiseWindow.repaint();

        mainWindow.revalidate();
        mainWindow.repaint();
    }
    public void paintFieldAtFirst(GameConfig gameConfig, ArrayList<PlayerStatistic> data) {
        returnToPrevView();
        int gridSize = 25;
        boardView = new BoardView(gameConfig.getWidth(), gameConfig.getHeight(), gridSize,
                actionWindow.getWidth() / 2 - (gridSize * gameConfig.getWidth() / 2), gridSize);
        actionWindow.add(boardView.getField(), BorderLayout.CENTER);
        boardView.getField().repaint();
        boardView.getField().revalidate();

        gameStatistics = new GameStatistics(this, mainFrame);
        gameStatistics.printTable(data);
        gameStatistics.getTable().setVisible(true);
        choiseWindow.remove(mainMenu.getMenu());
        choiseWindow.add(gameStatistics.getTable(), BorderLayout.CENTER);

        // TODO returns false for some reason
        actionWindow.requestFocusInWindow();

        actionWindow.addKeyListener(actionWindowListener);

        actionWindow.revalidate();
        choiseWindow.revalidate();
        mainFrame.revalidate();
    }
    public void displayError(String err) {
        JOptionPane.showMessageDialog(mainFrame, err);
    }
    public void quitGame() {
        returnToPrevView();
        boardView = null;
        gameStatistics = null;
    }
}
