package org.nsu.snake.client.view.UIcomponents;

import org.nsu.snake.client.view.ClientGUI;
import org.nsu.snake.model.GameConfig;
import org.nsu.snake.model.components.GamePlayer;
import org.nsu.snake.model.components.NodeRole;
import org.nsu.snake.model.components.PlayerStatistic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
public class MainMenu implements ActionListener{
    private final Button newGameButton;
//    private Button ratingButton;
//    private Button currentGameButton;
    private Button gameListButton;
    private Button exitButton;

    private ClientGUI clientGUI;

    private final Box sideBar;
    private final JFrame mainFrame;
    public MainMenu(JFrame mainFrame, ClientGUI parentGUI) {
        this.mainFrame = mainFrame;
        clientGUI = parentGUI;
        sideBar = Box.createVerticalBox();

        Color defaultColor = Color.black;
        Font defaultFont = new Font("Arial", Font.BOLD, 20);
        Dimension minSize = new Dimension(300, 100);
        Dimension prefSize = new Dimension(300, 100);
        Dimension maxSize = new Dimension(300, 100);

        newGameButton = new Button("New Game", defaultColor, prefSize, minSize, maxSize, defaultFont, this);
//        ratingButton = new Button("Rating", defaultColor, prefSize, minSize, maxSize, defaultFont, this);
//        currentGameButton = new Button("Current Game", defaultColor, prefSize, minSize, maxSize, defaultFont, this);
        gameListButton = new Button("Game List", defaultColor, prefSize, minSize, maxSize, defaultFont, this);
        exitButton = new Button("Exit", defaultColor, prefSize, minSize, maxSize, defaultFont, this);

        sideBar.add(newGameButton.getButton());

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dm = toolkit.getScreenSize();
        int fillerHeight = (dm.height - 400 - 25) / 5;
        Dimension fillerDim = new Dimension(5, fillerHeight);
        Box.Filler filler_1 = new Box.Filler(fillerDim, fillerDim, fillerDim);
        Box.Filler filler_2 = new Box.Filler(fillerDim, fillerDim, fillerDim);
        Box.Filler filler_3 = new Box.Filler(fillerDim, fillerDim, fillerDim);
        Box.Filler filler_4 = new Box.Filler(fillerDim, fillerDim, fillerDim);

        sideBar.add(filler_1);
        sideBar.add(gameListButton.getButton());
//        sideBar.add(filler_2);
//        sideBar.add(currentGameButton.getButton());
//        sideBar.add(filler_3);
//        sideBar.add(ratingButton.getButton());
        sideBar.add(filler_4);
        sideBar.add(exitButton.getButton());

        sideBar.setVisible(false);

    }
    public void showMenu(){
        sideBar.setVisible(true);
    }

    public void hideMenu(){
        sideBar.setVisible(false);
    }

    public Box getMenu(){
        return this.sideBar;
    }

    public void setGUI(ClientGUI gui) {
        this.clientGUI = gui;
    }

    private GameConfig gameConfig;
    private GamePlayer gamePlayer;
    private String gameName;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == newGameButton.getButton()) {
            LoginDialog loginDialog = new LoginDialog(mainFrame);
            loginDialog.setVisible(true);
            if (loginDialog.isSucceeded()) {
                ArrayList<PlayerStatistic> data = new ArrayList<>();
                data.add(new PlayerStatistic(gamePlayer.getName(), 0, NodeRole.MASTER));
                clientGUI.paintFieldAtFirst(gameConfig, data);
                clientGUI.clientMain.startNewGame(gameConfig, gamePlayer, gameName);
            }
            else {
                String err = loginDialog.getErrorMessage();
                if (err == null) err = "Try again";
                JOptionPane.showMessageDialog(mainFrame, err);
            }
        }
        if (e.getSource() == gameListButton.getButton()) {
            if (clientGUI.boardView == null) { // TODO придется занулять это поле после выхода из игры
                clientGUI.showGamesList(clientGUI.clientMain.getGamesAround());
            }
            else {
                clientGUI.actionWindow.requestFocusInWindow();
            }
        }
        if (e.getSource() == exitButton.getButton()) {
            mainFrame.dispose();
            clientGUI.clientMain.quitGame();
            clientGUI.clientMain.quitMulticast();
        }
    }

    private class LoginDialog extends JDialog {
        private final JTextField gameWidthField;
        private final JTextField gameHeightField;
        private final JTextField gameFoodStaticField;
        private final JTextField gameDelayField;
        private final JTextField playerNameField;
        private final JTextField gameNameField;
        private String errorMessage = null;
        private boolean succeeded;

        public LoginDialog(JFrame parent) {
            super(parent, "Login", true);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints cs = new GridBagConstraints();

            cs.fill = GridBagConstraints.HORIZONTAL;

            JLabel lbWidth = new JLabel("Set width: ");
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 1;
            panel.add(lbWidth, cs);

            gameWidthField = new JTextField(20);
            gameWidthField.setText("40");
            cs.gridx = 1;
            cs.gridy = 0;
            cs.gridwidth = 2;
            panel.add(gameWidthField, cs);

            JLabel lbHeight = new JLabel("Set height: ");
            cs.gridx = 0;
            cs.gridy = 1;
            cs.gridwidth = 1;
            panel.add(lbHeight, cs);

            gameHeightField = new JTextField(20);
            gameHeightField.setText("30");
            cs.gridx = 1;
            cs.gridy = 1;
            cs.gridwidth = 2;
            panel.add(gameHeightField, cs);

            JLabel lbFoodStatic = new JLabel("Set food static: ");
            cs.gridx = 0;
            cs.gridy = 2;
            cs.gridwidth = 1;
            panel.add(lbFoodStatic, cs);

            gameFoodStaticField = new JTextField(20);
            gameFoodStaticField.setText("1");
            cs.gridx = 1;
            cs.gridy = 2;
            cs.gridwidth = 2;
            panel.add(gameFoodStaticField, cs);

            JLabel lbDelay = new JLabel("Set delay: ");
            cs.gridx = 0;
            cs.gridy = 3;
            cs.gridwidth = 1;
            panel.add(lbDelay, cs);

            gameDelayField = new JTextField(20);
            gameDelayField.setText("1000");
            cs.gridx = 1;
            cs.gridy = 3;
            cs.gridwidth = 2;
            panel.add(gameDelayField, cs);

            JLabel lbName = new JLabel("Enter name : ");
            cs.gridx = 0;
            cs.gridy = 4;
            cs.gridwidth = 1;
            panel.add(lbName, cs);

            playerNameField = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 4;
            cs.gridwidth = 2;
            panel.add(playerNameField, cs);

            JLabel lbGameName = new JLabel("Enter game name : ");
            cs.gridx = 0;
            cs.gridy = 5;
            cs.gridwidth = 1;
            panel.add(lbGameName, cs);

            gameNameField = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 5;
            cs.gridwidth = 2;
            panel.add(gameNameField, cs);

            panel.setBorder(new LineBorder(Color.GRAY));

            JButton btnEnter = new JButton("Confirm");
            cs.gridx = 0;
            cs.gridy = 6;
            cs.gridwidth = 3;
            panel.add(btnEnter, cs);

            JButton btnExit = new JButton("Exit");
            cs.gridx = 0;
            cs.gridy = 7;
            cs.gridwidth = 3;
            panel.add(btnExit, cs);

            getContentPane().add(panel, BorderLayout.CENTER);

            btnEnter.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {

                    if (getGameWidth() < 10 || getGameWidth() > 100 ||
                        getGameHeight() < 10 || getGameHeight() > 100 ||
                        getGameFoodStatic() < 0 || getGameFoodStatic() > 100 ||
                        getGameDelay() < 100 || getGameDelay() > 3000) {
                        succeeded = false;
                        errorMessage = "Invalid config parameters";
                        dispose();
                        return;
                    }
                    if (! clientGUI.clientMain.gameNameIsUnique(getGameName())) {
                        errorMessage = "Game with this name already exists";
                        succeeded = false;
                        dispose();
                        return;
                    }
                    if (getPlayerName().isEmpty() || getGameName().isEmpty()) {
                        errorMessage = "Empty game/player name";
                        succeeded = false;
                        dispose();
                        return;
                    }
                    gameConfig = new GameConfig(getGameWidth(), getGameHeight(), getGameFoodStatic(), getGameDelay());
                    gamePlayer = new GamePlayer(getPlayerName());
                    gameName = gameNameField.getText();
                    succeeded = true;
                    dispose();
                }
            });

            btnExit.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    succeeded = false;
                    dispose();
                }
            });

            pack();
            setResizable(false);
            setLocationRelativeTo(parent);
        }

        private int getGameWidth() {
            return Integer.parseInt(gameWidthField.getText());
        }

        private int getGameHeight() {
            return Integer.parseInt(gameHeightField.getText());
        }

        private int getGameFoodStatic() {
            return Integer.parseInt(gameFoodStaticField.getText());
        }

        private int getGameDelay() {
            return Integer.parseInt(gameDelayField.getText());
        }
        private String getGameName() {return this.gameNameField.getText();}
        private String getPlayerName() {return playerNameField.getText();}
        public boolean isSucceeded() {
            return succeeded;
        }

        public String getErrorMessage() {return this.errorMessage;}
    }
}
