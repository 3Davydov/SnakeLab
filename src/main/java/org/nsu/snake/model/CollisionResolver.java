package org.nsu.snake.model;

import org.nsu.snake.model.components.Cell;
import org.nsu.snake.model.components.Direction;
import org.nsu.snake.model.components.Food;
import org.nsu.snake.model.components.Snake;

import java.util.ArrayList;
import java.util.LinkedList;

public class CollisionResolver {
    public boolean isNextSnakeStateIntersectsSnake(Cell nextHeadPosition, Direction direction, ArrayList<Snake> snakes) {
        switch (direction) {
            case UP -> nextHeadPosition.y -= 1;
            case DOWN -> nextHeadPosition.y += 1;
            case RIGHT -> nextHeadPosition.x += 1;
            case LEFT -> nextHeadPosition.x -= 1;
        }

        for (int i = 0; i < snakes.size(); i++) {
            Snake nextSnake = snakes.get(i);
            Cell cell = new Cell(0, 0);
            for (int j = 0; j < nextSnake.getBody().size(); j++) {
                cell.x += nextSnake.getBody().get(j).x;
                cell.y += nextSnake.getBody().get(j).y;
                if ((cell.x == nextHeadPosition.x) && (cell.y == nextHeadPosition.y)) return true;
            }
        }
        return false;
    }
    public ResolverAnswer isNextSnakeStateIntersectsFood(Cell nextHeadPosition, Direction direction, ArrayList<Food> food) {
        switch (direction) {
            case UP -> nextHeadPosition.y -= 1;
            case DOWN -> nextHeadPosition.y += 1;
            case RIGHT -> nextHeadPosition.x += 1;
            case LEFT -> nextHeadPosition.x -= 1;
        }
        for (int i = 0; i < food.size(); i++) {
            if ((nextHeadPosition.x == food.get(i).getPosition().x) && (nextHeadPosition.y == food.get(i).getPosition().y)) {
                ResolverAnswer answer = new ResolverAnswer();
                answer.isIntersects = true;
                answer.whichIntersect = food.get(i);
                return answer;
            }
        }
        ResolverAnswer answer = new ResolverAnswer();
        answer.isIntersects = false;
        answer.whichIntersect = null;
        return answer;
    }
    public boolean isFoodIntersectsSnake(Food food, Snake snake) {
        ArrayList<Cell> snakeBody = snake.getBody();
        int index = 0;
        Cell leftKeyCell;
        Cell rightKeyCell;
        Cell foodPosition = new Cell(food.getPosition());
        while (true) {
            try {
                leftKeyCell = new Cell(snakeBody.get(index));
                index++;
                rightKeyCell = new Cell(snakeBody.get(index));

                rightKeyCell.x += leftKeyCell.x;
                rightKeyCell.y += leftKeyCell.y;
                boolean isHorizontal = (leftKeyCell.y == rightKeyCell.y);

                if (CellLiesOnInterval(findLeftBorder(leftKeyCell, rightKeyCell, isHorizontal),
                                            findRightBorder(leftKeyCell, rightKeyCell, isHorizontal),
                                            foodPosition, isHorizontal))
                    return true;
            } catch (IndexOutOfBoundsException exception) {
                break;
            }
        }

        return false;
    }
    private boolean CellLiesOnInterval(Cell leftBorder, Cell rightBorder, Cell targetCell, boolean isHorizontal) {
        if (isHorizontal) {
            if (targetCell.y != leftBorder.y) return false;
            return (targetCell.x >= leftBorder.x && targetCell.x <= rightBorder.x);
        }
        else {
            if (targetCell.x != leftBorder.x) return false;
            return (targetCell.y >= leftBorder.y && targetCell.y <= rightBorder.y);
        }
    }
    private Cell findLeftBorder(Cell a, Cell b, boolean isHorizontal) {
        if (isHorizontal) {
            if (a.x <= b.x) return a;
            else return b;
        }
        else {
            if (a.y <= b.y) return a;
            else return b;
        }
    }
    private Cell findRightBorder(Cell a, Cell b, boolean isHorizontal) {
        if (isHorizontal) {
            if (a.x <= b.x) return b;
            else return a;
        }
        else {
            if (a.y <= b.y) return b;
            else return a;
        }
    }
    public class ResolverAnswer {
        public boolean isIntersects;
        public Object whichIntersect;
    }
}
