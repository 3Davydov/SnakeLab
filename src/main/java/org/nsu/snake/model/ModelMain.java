package org.nsu.snake.model;

import org.nsu.snake.model.components.*;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

import java.util.ArrayList;

public class ModelMain {
    private GameBoard gameBoard;
    private GameConfig gameConfig;
    private int stateOrder = 0;

    public ModelMain(GameConfig gameConfig, GamePlayer gamePlayer, NodeRole role, String gameName) {
        this.gameConfig = gameConfig;
        gameBoard = new GameBoard(gameConfig, gamePlayer, role, gameName);
    }

    public void incrementStateOrder() {
        stateOrder += 1;
    }

    public SnakesProto.GameMessage getGameState() {
        SnakesProto.GameState.Builder gameStateBuilder = SnakesProto.GameState.newBuilder();
        stateOrder += 1;
        gameStateBuilder.setStateOrder(stateOrder);

        ArrayList<Snake> snakes = gameBoard.getSnakes();
        for (int i = 0; i < snakes.size(); i++) {
            SnakesProto.GameState.Snake.Builder snakeBuilder = SnakesProto.GameState.Snake.newBuilder();
            snakeBuilder.setState(SnakesProto.GameState.Snake.SnakeState.ALIVE);
            snakeBuilder.setPlayerId(i + 1);
            switch (snakes.get(i).getDirection()) {
                case UP -> snakeBuilder.setHeadDirection(SnakesProto.Direction.UP);
                case DOWN -> snakeBuilder.setHeadDirection(SnakesProto.Direction.DOWN);
                case LEFT -> snakeBuilder.setHeadDirection(SnakesProto.Direction.LEFT);
                case RIGHT -> snakeBuilder.setHeadDirection(SnakesProto.Direction.RIGHT);
            }

            ArrayList<Cell> sourceKeyPoints = snakes.get(i).getKeyPoints();
            for (int j = 0; j < sourceKeyPoints.size(); j++) {
                SnakesProto.GameState.Coord.Builder snakeCoordBuilder = SnakesProto.GameState.Coord.newBuilder();
                snakeCoordBuilder.setX(sourceKeyPoints.get(j).x);
                snakeCoordBuilder.setY(sourceKeyPoints.get(j).y);
                snakeBuilder.addPoints(snakeCoordBuilder);
            }
            gameStateBuilder.addSnakes(snakeBuilder);
        }

        ArrayList<Food> foods = gameBoard.getFoods();
        for (int i = 0; i < foods.size(); i++) {
            SnakesProto.GameState.Coord.Builder foodBuilder = SnakesProto.GameState.Coord.newBuilder();
            foodBuilder.setX(foods.get(i).getPosition().x);
            foodBuilder.setY(foods.get(i).getPosition().y);
            gameStateBuilder.addFoods(foodBuilder);
        }

        SnakesProto.GamePlayers.Builder playersBuilder = SnakesProto.GamePlayers.newBuilder();
        for (int i = 0; i < snakes.size(); i++) {
            SnakesProto.GamePlayer.Builder playerBuilder = SnakesProto.GamePlayer.newBuilder();
            GamePlayer nextPlayer = gameBoard.getGamePlayer(snakes.get(i));
            playerBuilder.setId(nextPlayer.getId());
            playerBuilder.setName(nextPlayer.getName());
            switch (nextPlayer.getNodeRole()) {
                case MASTER -> playerBuilder.setRole(SnakesProto.NodeRole.MASTER);
                case NORMAL -> playerBuilder.setRole(SnakesProto.NodeRole.NORMAL);
                case DEPUTY -> playerBuilder.setRole(SnakesProto.NodeRole.DEPUTY);
                case VIEWER -> playerBuilder.setRole(SnakesProto.NodeRole.VIEWER);
            }
            playerBuilder.setScore(nextPlayer.getScore());

            playersBuilder.addPlayers(playerBuilder);
        }

        gameStateBuilder.setPlayers(playersBuilder);
        SnakesProto.GameMessage.StateMsg.Builder stateBuilder = SnakesProto.GameMessage.StateMsg.newBuilder();
        stateBuilder.setState(gameStateBuilder);

        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        gameMessageBuilder.setState(stateBuilder);

        gameMessageBuilder.setMsgSeq(stateOrder);

        return gameMessageBuilder.build();
    }
    public SnakesProto.GameMessage getGameAnnouncement() {
        SnakesProto.GameAnnouncement.Builder gameAnnouncementBuilder = SnakesProto.GameAnnouncement.newBuilder();

        ArrayList<Snake> snakes = gameBoard.getSnakes();
        SnakesProto.GamePlayers.Builder playersBuilder = SnakesProto.GamePlayers.newBuilder();
        for (int i = 0; i < snakes.size(); i++) {
            SnakesProto.GamePlayer.Builder playerBuilder = SnakesProto.GamePlayer.newBuilder();
            GamePlayer nextPlayer = gameBoard.getGamePlayer(snakes.get(i));
            playerBuilder.setId(nextPlayer.getId());
            playerBuilder.setName(nextPlayer.getName());
            switch (nextPlayer.getNodeRole()) {
                case MASTER -> playerBuilder.setRole(SnakesProto.NodeRole.MASTER);
                case NORMAL -> playerBuilder.setRole(SnakesProto.NodeRole.NORMAL);
                case DEPUTY -> playerBuilder.setRole(SnakesProto.NodeRole.DEPUTY);
                case VIEWER -> playerBuilder.setRole(SnakesProto.NodeRole.VIEWER);
            }
            playerBuilder.setScore(nextPlayer.getScore());

            playersBuilder.addPlayers(playerBuilder);
        }
        gameAnnouncementBuilder.setPlayers(playersBuilder);

        SnakesProto.GameConfig.Builder gameConfigBuilder = SnakesProto.GameConfig.newBuilder();
        GameConfig gameConfig = gameBoard.getGameConfig();
        gameConfigBuilder.setWidth(gameConfig.getWidth());
        gameConfigBuilder.setHeight(gameConfig.getHeight());
        gameConfigBuilder.setFoodStatic(gameConfig.getFoodStatic());
        gameConfigBuilder.setStateDelayMs(gameConfig.getStateDelayMs());
        gameAnnouncementBuilder.setConfig(gameConfigBuilder);

        gameAnnouncementBuilder.setCanJoin(true);
        gameAnnouncementBuilder.setGameName(gameBoard.getGameName());

        SnakesProto.GameMessage.AnnouncementMsg.Builder announcementMessageBuilder = SnakesProto.GameMessage.AnnouncementMsg.newBuilder();
        announcementMessageBuilder.addGames(gameAnnouncementBuilder);

        SnakesProto.GameMessage.Builder gameMessageBuilder = SnakesProto.GameMessage.newBuilder();
        gameMessageBuilder.setAnnouncement(announcementMessageBuilder);

        gameMessageBuilder.setMsgSeq(stateOrder);

        return gameMessageBuilder.build();
    }
    public NodeRole getNodeRole(GamePlayer gamePlayer) {
        return gameBoard.getNodeRole(gamePlayer);
    }
    public void calculateNextState(Direction direction, GamePlayer gamePlayer) {
        gameBoard.calculateNextState(direction, gamePlayer);
    }
}
