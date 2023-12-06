package org.nsu.snake.model.components;

public class GamePlayer {
    private String name = "Default";
    private int id = -1;
    private String ipAddress;
    private int port;
    private NodeRole nodeRole;
    private final PlayerType playerType = PlayerType.HUMAN;
    private int score = 0;

    public GamePlayer(String name) {
        this.name = name;
    }

    public GamePlayer(GamePlayer src) {
        this.name = src.getName();
        this.id = src.getId();
        this.nodeRole = src.getNodeRole();
        this.score = src.score;
        this.ipAddress = src.ipAddress;
        this.port = src.port;
    }

    public void setID(int newID) {
        id = newID;
    }

    public void setNodeRole(NodeRole newRole) {
        nodeRole = newRole;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public NodeRole getNodeRole() {
        return nodeRole;
    }

    public int getScore() {
        return score;
    }

    public void incrementScore() {score += 1;}
}
