package org.nsu.snake.client.view.UIcomponents;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.JButton;
public class Button {
    private JButton button;

    public Button(String name, Color newColor, Dimension newPrefSize, Dimension newMinSize, Dimension newMaxSize, Font newFont, ActionListener newAcL){

        button = new JButton(name);
        button.setBackground(newColor);
        button.setMaximumSize(newMaxSize);
        button.setPreferredSize(newPrefSize);
        button.setMinimumSize(newMinSize);
        button.setFont(newFont);
        button.setForeground(newColor);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.addActionListener(newAcL);
    }

    public JButton getButton(){
        return this.button;
    }
}
