package org.nsu.snake.model.components;

public class Food {
    private Cell position;
    public Food (Cell position) {
        this.position = position;
    }
    public Cell getPosition() {return this.position;}
}
