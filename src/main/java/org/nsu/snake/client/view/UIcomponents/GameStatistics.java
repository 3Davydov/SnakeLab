package org.nsu.snake.client.view.UIcomponents;

import org.nsu.snake.client.view.ClientGUI;
import org.nsu.snake.model.components.GameInfo;
import org.nsu.snake.model.components.PlayerStatistic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

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
    private class VerticalLayout implements LayoutManager {

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


//        Color defaultColor = Color.black;
//        Font defaultFont = new Font("Arial", Font.BOLD, 20);
//        Dimension minSize = new Dimension(table.getWidth() - 30, 100);
//        Dimension prefSize = new Dimension(table.getWidth() - 30, 100);
//        Dimension maxSize = new Dimension(table.getWidth() - 30, 100);
//
//        for (int i = 0; i < data.size(); i++){
//            Button button = new Button(data.get(i).getStatistic(), defaultColor, prefSize, minSize, maxSize, defaultFont, null);
//            button.getButton().setVerticalAlignment((int) Component.CENTER_ALIGNMENT);
//            button.getButton().setBackground(Color.BLUE);
//            button.getButton().setVisible(true);
//            table.add(button.getButton());
//        }
//
//        table.revalidate();
//        table.repaint();

    }
}
