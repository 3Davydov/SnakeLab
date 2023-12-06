package org.nsu.snake.client.view;

import org.nsu.snake.client.ClientMain;
import org.nsu.snake.client.view.UIcomponents.BoardView;
import org.nsu.snake.client.view.UIcomponents.MainMenu;
import org.nsu.snake.model.ModelMain;
import org.nsu.snake.model.components.Direction;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ClientGUI {
    public JFrame mainFrame;
    private JPanel mainWindow;

    public JPanel actionWindow;
    private JPanel choiseWindow;
    private MainMenu mainMenu;
    public BoardView boardView;
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
            }
        };
    }

    public void repaintField(SnakesProto.GameState gameState) {
        boardView.repaintField(gameState);
        actionWindow.repaint();
    }

    public Direction getCurrentDirection() {
        staticDirection = currentDirection;
        return this.staticDirection;
    }
}
