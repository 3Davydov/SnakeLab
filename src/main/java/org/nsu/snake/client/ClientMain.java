package org.nsu.snake.client;

import org.nsu.snake.model.GameConfig;
public class ClientMain {
    private GameConfig gameConfig = null;
    public ClientMain() {}
    public void setConfig(GameConfig newGameConfig) {this.gameConfig = newGameConfig;}
    public GameConfig getConfig() {return this.gameConfig;}
}
