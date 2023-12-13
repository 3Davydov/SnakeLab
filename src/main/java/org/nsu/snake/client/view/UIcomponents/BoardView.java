package org.nsu.snake.client.view.UIcomponents;

import org.nsu.snake.proto.compiled_proto.SnakesProto;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JComponent;
public class BoardView {
    private int sizeX;
    private int sizeY;

    private Rectangle2D[][] cells;
    private Color[][] colorMap;
    private TField tField;

    private class TField extends JComponent {

        @Override
        public void paintComponent(Graphics g){
            Graphics2D graphics2d = (Graphics2D) g;
            graphics2d.setPaint(Color.BLACK);
            for (int i = 0; i < sizeX; i += 1)
                for (int j = 0; j < sizeY; j += 1){
                    graphics2d.setPaint(Color.BLACK);
                    graphics2d.draw(cells[i][j]);
                    if (colorMap[i][j] != null) {
                        graphics2d.setPaint(colorMap[i][j]);
                        graphics2d.fill(cells[i][j]);
                    }
                }
        }
    }

    public BoardView(int sizeX, int sizeY, int gridSize, int startLeftCoord, int startUpperCoord) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;

        tField = new TField();
        cells = new Rectangle2D[sizeX][sizeY];
        colorMap = new Color[sizeX][sizeY];

        for (int x = startLeftCoord, i = 0; i < sizeX; x += gridSize, i += 1)
            for (int y = startUpperCoord, j = 0; j < sizeY; y += gridSize, j += 1){
                cells[i][j] = new Rectangle2D.Double(x, y, gridSize, gridSize);
            }
    }
    public void repaintField(SnakesProto.GameState gameState) {
        colorMap = new Color[sizeX][sizeY];
        List<SnakesProto.GameState.Coord> foods = gameState.getFoodsList();
        for (int i = 0; i < foods.size(); i++) {
            colorMap[(foods.get(i).getX() % sizeX+ sizeX) % sizeX][(foods.get(i).getY() % sizeY + sizeY) % sizeY] = Color.RED;
        }

        List<SnakesProto.GameState.Snake> snakes = gameState.getSnakesList();
        for (int i = 0; i < snakes.size(); i++) {
            List<SnakesProto.GameState.Coord> coords = snakes.get(i).getPointsList();
            int posX = 0;
            int posY = 0;
            int prevPosX = -1;
            int prevPosY = -1;
            for (int j = 0; j < coords.size(); j++) {
                if (j == 0) {
                    posX = coords.get(0).getX();
                    posY = coords.get(0).getY();
                    prevPosX = posX;
                    prevPosY = posY;
                    continue;
                }
                else {
                    posX += coords.get(j).getX();
                    posY += coords.get(j).getY();
                }
                if (prevPosX == posX) {
                    repaintVerticalInterval(prevPosY, posY, posX);
                }
                if (prevPosY == posY) {
                    repaintHorizontalInterval(prevPosX, posX, posY);
                }
                prevPosX = posX;
                prevPosY = posY;
            }
        }
    }
    private void repaintVerticalInterval(int prevPosY, int posY, int constPosX) {
        int leftBorder = Math.min(prevPosY, posY);
        int rightBorder = Math.max(prevPosY, posY);
        for (int i = leftBorder; i <= rightBorder; i++) {
            colorMap[(constPosX % sizeX + sizeX) % sizeX][(i % sizeY + sizeY) % sizeY] = Color.BLUE;
        }
    }
    private void repaintHorizontalInterval(int prevPosX, int posX, int constPosY) {
        int leftBorder = Math.min(prevPosX, posX);
        int rightBorder = Math.max(prevPosX, posX);
        for (int i = leftBorder; i <= rightBorder; i++) {
            colorMap[(i % sizeX + sizeX) % sizeX][(constPosY % sizeY + sizeY) % sizeY] = Color.BLUE;
        }
    }
    public JComponent getField(){
        return this.tField;
    }
}
