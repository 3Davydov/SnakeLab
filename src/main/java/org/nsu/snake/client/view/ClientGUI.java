package org.nsu.snake.client.view;

import org.nsu.snake.client.ClientMain;
import org.nsu.snake.client.view.UIcomponents.MainMenu;

import javax.swing.*;
import java.awt.*;

public class ClientGUI {
    private JFrame mainFrame;
    private JPanel mainWindow;

    private JPanel actionWindow;
    private JPanel choiseWindow;
    private MainMenu mainMenu;

    public ClientMain clientMain;

    private JFrame getJFrame() {
        JFrame jframe = new JFrame() {};
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
    }

    public static void main(String[] args) {
        ClientMain clientMain = new ClientMain();
        ClientGUI clientGUI = new ClientGUI(clientMain);
    }
}
