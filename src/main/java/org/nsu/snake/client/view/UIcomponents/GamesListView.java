package org.nsu.snake.client.view.UIcomponents;

import org.nsu.snake.client.view.ClientGUI;
import org.nsu.snake.model.GameConfig;
import org.nsu.snake.model.components.GameInfo;
import org.nsu.snake.model.components.GamePlayer;
import org.nsu.snake.model.components.NodeRole;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.LineBorder;

public class GamesListView {
    private JPanel table;
    private ArrayList<GameInfo> data = null;
    private Map<JButton, GameInfo> buttonGameInfoMap;
    private ClientGUI clientGUI;
    private JFrame mainFrame;

    private String playerName;
    private String playerType;

    public GamesListView(ClientGUI clientGUI, JFrame mainFrame) {
        this.clientGUI = clientGUI;
        this.mainFrame = mainFrame;
        table = new JPanel();
        buttonGameInfoMap = new HashMap<>();
        table.setLayout(new VerticalLayout());
        table.setBackground(Color.GRAY);
        table.setVisible(false);
    }

    private class VerticalLayout implements LayoutManager{

        private int GAP = 5;

        @Override
        public void addLayoutComponent(String name, Component comp) {
            return;
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            return;
        }

        @Override
        public Dimension preferredLayoutSize(Container c) {
            return calculateBestSize(c);
        }
        @Override
        public Dimension minimumLayoutSize(Container c) {
            return calculateBestSize(c);
        }
        private Dimension size = new Dimension();
        private Dimension calculateBestSize(Container c) {
            Component[] comps = c.getComponents();
            int maxWidth = 0;
            for (Component comp : comps) {
                int width = comp.getWidth();
                if (width > maxWidth) maxWidth = width;
            }
            size.width = maxWidth + GAP;
            int height = 0;
            for (Component comp : comps) {
                height += GAP;
                height += comp.getHeight();
            }
            size.height = height;
            return size;
        }
        @Override
        public void layoutContainer(Container c) {
            Component comps[] = c.getComponents();
            int currentY = GAP;
            for (Component comp : comps) {
                Dimension pref = comp.getPreferredSize();
                comp.setBounds(GAP, currentY, pref.width, pref.height);
                currentY += GAP;
                currentY += pref.height;
            }
        }

    }
    public JPanel getTable() {
        return this.table;
    }
    public void printTable(ArrayList<GameInfo> newData) {
        this.data = newData;
        table.removeAll();

        Color defaultColor = Color.black;
        Font defaultFont = new Font("Arial", Font.BOLD, 20);
        Dimension minSize = new Dimension(table.getWidth() - 30, 100);
        Dimension prefSize = new Dimension(table.getWidth() - 30, 100);
        Dimension maxSize = new Dimension(table.getWidth() - 30, 100);

        for (int i = 0; i < data.size(); i++){
            Button button = new Button(data.get(i).getString(), defaultColor, prefSize, minSize, maxSize, defaultFont, null);
            button.getButton().setVerticalAlignment((int) Component.CENTER_ALIGNMENT);
            button.getButton().setBackground(Color.BLUE);
            button.getButton().setVisible(true);
            table.add(button.getButton());
            buttonGameInfoMap.put(button.getButton(), data.get(i));
            button.getButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    GameInfo requestGame = buttonGameInfoMap.get(button.getButton());
                    if (requestGame == null) return;

                    JoinDialog joinDialog = new JoinDialog(mainFrame);
                    joinDialog.setVisible(true);
                    if (joinDialog.isSucceeded()) {
                        clientGUI.clientMain.joinGame(requestGame, playerName, playerType);
                    }
                    else {
                        String err = joinDialog.getErrorMessage();
                        if (err == null) err = "Try again";
                        JOptionPane.showMessageDialog(mainFrame, err);
                    }
                }
            });
        }

        table.setVisible(true);
        table.revalidate();
        table.repaint();
    }

    private class JoinDialog extends JDialog {
        private final JTextField playerNameField;
        private final JTextField playerTypeFiled;
        private String errorMessage = null;
        private boolean succeeded;

        public JoinDialog(JFrame parent) {
            super(parent, "Join", true);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints cs = new GridBagConstraints();

            cs.fill = GridBagConstraints.HORIZONTAL;

            JLabel lbWidth = new JLabel("Set your name: ");
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 1;
            panel.add(lbWidth, cs);

            playerNameField = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 0;
            cs.gridwidth = 2;
            panel.add(playerNameField, cs);

            JLabel lbHeight = new JLabel("Set your role: ");
            cs.gridx = 0;
            cs.gridy = 1;
            cs.gridwidth = 1;
            panel.add(lbHeight, cs);

            playerTypeFiled = new JTextField(20);
            playerTypeFiled.setText("NORMAL");
            cs.gridx = 1;
            cs.gridy = 1;
            cs.gridwidth = 2;
            panel.add(playerTypeFiled, cs);

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

                    if (getPlayerName().isEmpty()) {
                        succeeded = false;
                        errorMessage = "Player name is empty";
                        dispose();
                        return;
                    }

                    NodeRole role = null;
                    switch (playerTypeFiled.getText()) {
                        case "NORMAL" -> role = NodeRole.NORMAL;
                        case "MASTER" -> role = NodeRole.MASTER;
                        case "DEPUTY" -> role = NodeRole.DEPUTY;
                        case "VIEWER" -> role = NodeRole.VIEWER;
                    }
                    if (role == null) {
                        succeeded = false;
                        errorMessage = "Invalid player type";
                        dispose();
                        return;
                    }

                    playerName = playerNameField.getText();
                    playerType = playerTypeFiled.getText();
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

        private String getPlayerName() {return playerNameField.getText();}

        private String getPlayerType() {return playerTypeFiled.getText();}
        public boolean isSucceeded() {
            return succeeded;
        }
        public String getErrorMessage() {return this.errorMessage;}
    }
}
