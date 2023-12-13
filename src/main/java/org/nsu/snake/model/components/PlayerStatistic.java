package org.nsu.snake.model.components;

public class PlayerStatistic {
    private String name;
    private int score;
    private String role;
    public PlayerStatistic(String name, int score, NodeRole role) {
        this.name = name;
        this.score = score;

        switch (role) {
            case MASTER -> this.role = "MASTER";
            case DEPUTY -> this.role = "DEPUTY";
            case NORMAL -> this.role = "NORMAL";
            case VIEWER -> this.role = "VIEWER";
        }
    }
    public String getName() {return this.name;}
    public int getScore() {return this.score;}
    public String getRole() {return this.role;}

    public String getStatistic() {
        return (name + " " + String.valueOf(score) + " [" + role + "]");
    }
}
