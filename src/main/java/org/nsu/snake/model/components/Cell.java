package org.nsu.snake.model.components;

public class Cell {
    public int x;
    public int y;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Cell() {}

    public Cell(Cell src) {
        this.x = src.x;
        this.y = src.y;
    }
}
