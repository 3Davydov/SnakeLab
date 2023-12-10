package org.nsu.snake.model.components;

public class GameInfo {
    private String leader;
    private String leaderIP;
    private String size;
    private String food;
    private int port;
    private final String resultStr;
    public GameInfo(String leader, String leaderIP, int width, int height, int playersNum, int foodStatic, int port) {
        this.leaderIP = leaderIP;
        this.port = port;
        this.leader = leader;
        resultStr = leader + "   [" + leaderIP + "]   " + String.valueOf(width) + "x" + String.valueOf(height) + "   " +
                String.valueOf(playersNum) + " + " + String.valueOf(foodStatic);
    }
    public String getLeader() {
        return this.leader;
    }
    public String getLeaderIP() {
        return this.leaderIP;
    }
    public String getString() {
        return resultStr;
    }
    public int getPort() {return this.port;}
}
