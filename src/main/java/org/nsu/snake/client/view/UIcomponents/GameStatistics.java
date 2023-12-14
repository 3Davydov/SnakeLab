package org.nsu.snake.client.view.UIcomponents;

import org.nsu.snake.client.view.ClientGUI;
import org.nsu.snake.model.components.PlayerStatistic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class GameStatistics {
    private ClientGUI clientGUI;
    private JFrame mainFrame;
    private JPanel table;
    private ArrayList<PlayerStatistic> data;

    private Button exitButton;
    public GameStatistics(ClientGUI clientGUI, JFrame mainFrame) {
        this.clientGUI = clientGUI;
        this.mainFrame = mainFrame;
        this.data = new ArrayList<>();
        table = new JPanel();
        table.setLayout(new GridLayout(0, 1));
        table.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setBackground(Color.YELLOW);

        Color defaultColor = Color.black;
        Font defaultFont = new Font("Arial", Font.BOLD, 20);
        Dimension minSize = new Dimension(300, 100);
        Dimension prefSize = new Dimension(300, 100);
        Dimension maxSize = new Dimension(300, 100);

        exitButton = new Button("Exit", defaultColor, prefSize, minSize, maxSize, defaultFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == exitButton.getButton()) {
                    clientGUI.clientMain.quitGame();
                    clientGUI.quitGame();
                }
            }
        });
    }
    public JPanel getTable() {
        return this.table;
    }
    public void printTable(ArrayList<PlayerStatistic> newData) {
        this.data = newData;
        table.removeAll();

        for (PlayerStatistic p : newData) {
            JTextField textField = new JTextField(p.getStatistic());
            textField.setEditable(false);
            textField.setFont(new Font("Arial", Font.BOLD, 20));

            Dimension fixedSize = new Dimension(300, 50);
            textField.setPreferredSize(fixedSize);

            textField.setForeground(Color.BLACK);
            textField.setBackground(Color.YELLOW);
            textField.setHorizontalAlignment(SwingConstants.CENTER);
            table.add(textField);
        }

        table.add(exitButton.getButton());
    }
}
