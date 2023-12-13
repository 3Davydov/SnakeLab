package org.nsu.snake.model.components;

import org.nsu.snake.proto.compiled_proto.SnakesProto;

import java.util.Objects;
public class GamePlayer {
    private String name = "Default";
    private int id = -1;
    private String ipAddress;
    private int port;
    private NodeRole nodeRole;
    private final PlayerType playerType = PlayerType.HUMAN;
    private int score = 0;
    private Direction direction;
    private String hostIP;
    private int hostPort;

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
        this.direction = src.direction;
    }

    public GamePlayer(String name, int id, String ip, int port, NodeRole role, int score) {
        this.name = name;
        this.id = id;
        this.ipAddress = ip;
        this.port = port;
        this.nodeRole = role;
        this.score = score;
    }

    public GamePlayer(SnakesProto.GamePlayer src) {
        this.name = src.getName();
        this.id = src.getId();
        this.ipAddress = src.getIpAddress();
        this.port = src.getPort();

        switch (src.getRole()) {
            case MASTER -> this.nodeRole = NodeRole.MASTER;
            case DEPUTY -> this.nodeRole = NodeRole.DEPUTY;
            case NORMAL -> this.nodeRole = NodeRole.NORMAL;
            case VIEWER -> this.nodeRole = NodeRole.VIEWER;
        }

        this.score = src.getScore();
    }

    public void setHost(String ip, int port) {
        this.hostIP = ip;
        this.hostPort = port;
    }

    public int getHostPort() {return this.hostPort;}
    public String getHostIP() {return this.hostIP;}
    public void setID(int newID) {
        System.out.println("ID CHANGED " + id + " -> " + newID);
        id = newID;
    }
    public void setNodeRole(NodeRole newRole) {
        nodeRole = newRole;
    }
    public void setIP(String newIP) {
        ipAddress = newIP;
    }
    public void setPort(int newPort) {
        port = newPort;
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
    public void incrementScore() {score += 1;} // TODO не забываем
    public void setDirection(Direction newDirection) {
        this.direction = newDirection;
    }
    public Direction getDirection() {
        return this.direction;
    }
    public int getPort() {
        return this.port;
    }
    public String getIpAddress() {
        return this.ipAddress;
    }
    public void nullifySore() {
        this.score = 0;
    }

    public PlayerType getPlayerType() {return this.playerType;}
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GamePlayer gamePlayer = (GamePlayer) o;
        if (Objects.equals(this.name, gamePlayer.name) &&
                this.id == gamePlayer.getId() &&
                Objects.equals(this.ipAddress, gamePlayer.getIpAddress()) &&
                this.port == gamePlayer.getPort() &&
                this.nodeRole == gamePlayer.getNodeRole() &&
                this.playerType == gamePlayer.getPlayerType()) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(port, ipAddress, name, score, nodeRole, playerType);
    }
}
