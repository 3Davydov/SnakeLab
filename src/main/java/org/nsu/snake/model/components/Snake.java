package org.nsu.snake.model.components;

import org.nsu.snake.client.management.TrackedNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

public class Snake {
    private ArrayList<Cell> body;
    private Direction direction;
    private final int fieldWidth;
    private final int fieldHeight;
    private SnakeState snakeState;
    private int playerId;

    public Snake(Cell head, Direction direction, int newWidth, int newHeight, SnakeState state) {
        body = new ArrayList<>();
        body.add(head);
        body.add(new Cell(1, 0));
        this.direction = direction;
        this.fieldWidth = newWidth;
        this.fieldHeight = newHeight;
        this.snakeState = state;
    }

    public Snake(ArrayList<Cell> body, Direction direction, int newWidth, int newHeight, SnakeState state) {
        this.body = new ArrayList<>(body);
        this.direction = direction;
        this.fieldWidth = newWidth;
        this.fieldHeight = newHeight;
        this.snakeState = state;
    }

    public Snake() {
        this.snakeState = SnakeState.VIEWER;
        fieldHeight = 0;
        fieldWidth = 0;
    }

    // We use player ID only for dead player's snake (because this player already deleted from Map)
    public void setPlayerID(int id) {
        this.playerId = id;
    }
    public int getPlayerId() {return this.playerId;}

    public SnakeState getSnakeState() {return this.snakeState;}

    public void setSnakeState(SnakeState state) {this.snakeState = state;}
    public ArrayList<Cell> getBody() {
        @SuppressWarnings("uncheked")
        ArrayList<Cell> ret = (ArrayList<Cell>) this.body.clone();
        return ret;
    }
    public ArrayList<Cell> getKeyPoints() {
        ArrayList<Cell> ret = new ArrayList<>();
        Cell lastKeyPoint = new Cell(body.get(0));
        ret.add(lastKeyPoint);

        int realX = lastKeyPoint.x;
        int realY = lastKeyPoint.y;
        boolean horizontal = false;
        Cell nextKeyPoint = null;
        Cell prevPoint = null;
        if (body.size() > 2) {
            for (int i = 1; i < body.size(); i++) {
                nextKeyPoint = new Cell(body.get(i));
                realX += nextKeyPoint.x;
                realY += nextKeyPoint.y;
                if (i == 1) {
                    horizontal = (realY == lastKeyPoint.y);
                    prevPoint = new Cell(realX, realY);
                } else {
                    if ((realY == prevPoint.y) != horizontal) {
                        horizontal = (!horizontal);
                        ret.add(new Cell(prevPoint.x - lastKeyPoint.x, prevPoint.y - lastKeyPoint.y));
                        lastKeyPoint = new Cell(prevPoint.x, prevPoint.y);
                    }
                    prevPoint = new Cell(realX, realY);
                }
            }
            ret.add(new Cell(prevPoint.x - lastKeyPoint.x, prevPoint.y - lastKeyPoint.y));
        }
        else
            ret.add(new Cell(body.get(body.size() - 1)));
//        if (body.size() > 1) {
//            System.out.println("Body :");
//            for (int i = 0; i < body.size(); i++) System.out.println(body.get(i).x + " " + body.get(i).y);
//            System.out.println();
//            System.out.println("Key :");
//            for (int i = 0; i < ret.size(); i++) System.out.println(ret.get(i).x + " " + ret.get(i).y);
//            System.out.println("--------------------");
//        }
        return ret;
    }
    public void moveSnake(Direction newDirection) {
        Cell newHead = new Cell(body.get(0));
        Cell oldHead = body.get(0);
        switch (newDirection) {
            case LEFT -> {
                newHead.x -= 1;
                newHead.x = (newHead.x % fieldWidth + fieldWidth) % fieldWidth;
                oldHead.y = 0;
                oldHead.x = 1;
            }
            case RIGHT -> {
                newHead.x += 1;
                newHead.x = (newHead.x % fieldWidth + fieldWidth) % fieldWidth;
                oldHead.y = 0;
                oldHead.x = -1;
            }
            case UP -> {
                newHead.y -= 1;
                newHead.y = (newHead.y % fieldHeight + fieldHeight) % fieldHeight;
                oldHead.y = 1;
                oldHead.x = 0;
            }
            case DOWN -> {
                newHead.y += 1;
                newHead.y = (newHead.y % fieldHeight + fieldHeight) % fieldHeight;
                oldHead.y = -1;
                oldHead.x = 0;
            }
        }

        body.add(0, newHead);
        body.remove(body.size() - 1);
        direction = newDirection;
    }
    public void addPoint(Direction newDirection) {
        Cell newHead = new Cell(body.get(0));
        Cell oldHead = body.get(0);
        switch (newDirection) {
            case LEFT -> {
                newHead.x -= 1;
                newHead.x = (newHead.x % fieldWidth + fieldWidth) % fieldWidth;
                oldHead.y = 0;
                oldHead.x = 1;
            }
            case RIGHT -> {
                newHead.x += 1;
                newHead.x = (newHead.x % fieldWidth + fieldWidth) % fieldWidth;
                oldHead.y = 0;
                oldHead.x = -1;
            }
            case UP -> {
                newHead.y -= 1;
                newHead.y = (newHead.y % fieldHeight + fieldHeight) % fieldHeight;
                oldHead.y = 1;
                oldHead.x = 0;
            }
            case DOWN -> {
                newHead.y += 1;
                newHead.y = (newHead.y % fieldHeight + fieldHeight) % fieldHeight;
                oldHead.y = -1;
                oldHead.x = 0;
            }
        }
        for (int i = 0; i < body.size(); i++) {
            if (newHead.x == body.get(i).x && newHead.y == body.get(i).y) {
                moveSnake(direction);
                return;
            }
        }
        body.add(0, newHead);
        direction = newDirection;
    }
    public void printSnake() {
        for (int i = 0; i < body.size(); i++) {
            System.out.print(body.get(i).x + " ");
            System.out.print(body.get(i).y);
            System.out.println();
        }
        System.out.println();
    }
    public void setDirection(Direction newDirection) {
        this.direction = newDirection;
    }
    public Direction getDirection() {
        return this.direction;
    }
}
