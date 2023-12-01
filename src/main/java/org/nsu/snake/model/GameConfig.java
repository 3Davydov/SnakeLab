package org.nsu.snake.model;

public class GameConfig {
    private int width = 40;
    private int height = 30;
    private int foodStatic = 1;
    private int stateDelayMs = 1000;
    public GameConfig(int newWidth, int newHeight, int newFoodStatic, int newDelay) {
        width = newWidth;
        height = newHeight;
        foodStatic = newFoodStatic;
        stateDelayMs = newDelay;
    }
    public GameConfig() {}
    public int getWidth() {return this.width;}
    public int getHeight() {return this.height;}
    public int getFoodStatic() {return this.foodStatic;}
    public int getStateDelayMs() {return this.stateDelayMs;}
}
