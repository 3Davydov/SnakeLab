package org.nsu.snake.model.components;

import org.nsu.snake.model.CollisionResolver;
import org.nsu.snake.model.GameConfig;
import org.nsu.snake.model.ModelMain;
import org.nsu.snake.proto.compiled_proto.SnakesProto;
import org.w3c.dom.Node;

import java.util.*;

public class GameBoard {
    private GameConfig gameConfig;
    private List<Snake> snakes;
    private List<Food> foods;
    private Map<Snake, GamePlayer> gamePlayerMap;
    private int nextID = 1;
    private CollisionResolver collisionResolver;
    private String gameName;
    private ModelMain modelMain;

    public GameBoard(GameConfig gameConfig, GamePlayer gamePlayer, NodeRole role, String gameName, ModelMain modelMain) {
        this.gameConfig = gameConfig;
        this.modelMain = modelMain;
        this.gameName = gameName;
        snakes = new ArrayList<>();
        foods = new ArrayList<>();
        collisionResolver = new CollisionResolver();

        Random random = new Random();
        Cell head = new Cell(Math.abs(random.nextInt() % (gameConfig.getWidth() - 1)), Math.abs(random.nextInt() % (gameConfig.getHeight() - 1)));
        Snake firstSnake = new Snake(head, Direction.LEFT, gameConfig.getWidth(), gameConfig.getHeight(), SnakeState.ALIVE);
        snakes.add(firstSnake);

        gamePlayer.setID(nextID);
        nextID++;
        gamePlayer.setNodeRole(role);
        gamePlayer.setDirection(Direction.LEFT);
        gamePlayerMap = new HashMap<>();
        gamePlayerMap.put(firstSnake, gamePlayer);

        for (int i = 0; i < gameConfig.getFoodStatic() + 1; i++) {
            Cell randFoodCell = new Cell((random.nextInt() % gameConfig.getWidth() + gameConfig.getWidth()) % gameConfig.getWidth(),
                    (random.nextInt() % gameConfig.getHeight() + gameConfig.getHeight()) % gameConfig.getHeight());
            if (! collisionResolver.isFoodIntersectsSnake(new Food(randFoodCell), firstSnake)) {
                foods.add(new Food(randFoodCell));
            }
        }
    }
    public GameBoard(GameConfig gameConfig, String gameName, ModelMain modelMain, SnakesProto.GameMessage sourceGame) {
        this.gameConfig = gameConfig;
        this.modelMain = modelMain;
        this.gameName = gameName;
        this.collisionResolver = new CollisionResolver();

        snakes = new ArrayList<>();
        gamePlayerMap = new HashMap<>();
        ArrayList<SnakesProto.GameState.Snake> srcSnakes = new ArrayList<>(sourceGame.getState().getState().getSnakesList());
        ArrayList<SnakesProto.GamePlayer> srcPlayers = new ArrayList<>(sourceGame.getState().getState().getPlayers().getPlayersList());

        for (int i = 0; i < srcSnakes.size(); i++) {
            ArrayList<SnakesProto.GameState.Coord> srcSnakeBody = new ArrayList<>(srcSnakes.get(i).getPointsList());
            ArrayList<Cell> newSnakeBody = buildSnakeByKeyPoints(srcSnakeBody);
            Direction newSnakeDirection = Direction.LEFT;
            switch (srcSnakes.get(i).getHeadDirection()) {
                case UP -> newSnakeDirection = Direction.UP;
                case DOWN -> newSnakeDirection = Direction.DOWN;
                case LEFT -> newSnakeDirection = Direction.LEFT;
                case RIGHT -> newSnakeDirection = Direction.RIGHT;
            }

            int playerID = srcSnakes.get(i).getPlayerId();
            SnakesProto.GamePlayer srcPlayer = getPlayerByID(playerID, srcPlayers);
            if (srcPlayer == null) {
                System.out.println("PLAYER WITH ID " + playerID + " DOES NOT EXIST");
                continue;
            }
            if (nextID <= playerID) nextID = (playerID + 1);
            NodeRole newPlayerRole = NodeRole.NORMAL;
            switch (srcPlayer.getRole()) {
                case MASTER -> newPlayerRole = NodeRole.MASTER;
                case NORMAL -> newPlayerRole = NodeRole.NORMAL;
                case VIEWER -> newPlayerRole = NodeRole.VIEWER;
                case DEPUTY -> newPlayerRole = NodeRole.DEPUTY;
            }
            if (newPlayerRole == NodeRole.MASTER) {
                Snake newSnake = new Snake(newSnakeBody, newSnakeDirection, gameConfig.getWidth(), gameConfig.getHeight(), SnakeState.ZOMBIE);
                snakes.add(newSnake);
                continue;
            }
            if (newPlayerRole == NodeRole.DEPUTY) newPlayerRole = NodeRole.MASTER;
            Snake newSnake = new Snake(newSnakeBody, newSnakeDirection, gameConfig.getWidth(), gameConfig.getHeight(), SnakeState.ALIVE);
            snakes.add(newSnake);
            gamePlayerMap.put(newSnake,
                    new GamePlayer(srcPlayer.getName(), srcPlayer.getId(), srcPlayer.getIpAddress(), srcPlayer.getPort(), newPlayerRole, srcPlayer.getScore()));
        }

        foods = new ArrayList<>();
        ArrayList<SnakesProto.GameState.Coord> srcFoods = new ArrayList<>(sourceGame.getState().getState().getFoodsList());
        for (int i = 0; i < srcFoods.size(); i++) {
            foods.add(new Food(new Cell(srcFoods.get(i).getX(), srcFoods.get(i).getY())));
        }
    }
    private ArrayList<Cell> buildSnakeByKeyPoints(ArrayList<SnakesProto.GameState.Coord> srcBody) {
        ArrayList<Cell> newSnakeBody = new ArrayList<>();
        int posX = 0;
        int posY = 0;
        int prevPosX = -1;
        int prevPosY = -1;
        for (int j = 0; j < srcBody.size(); j++) {
            if (j == 0) {
                posX = srcBody.get(0).getX();
                posY = srcBody.get(0).getY();
                prevPosX = posX;
                prevPosY = posY;
                newSnakeBody.add(new Cell(posX, posY));
                continue;
            }
            else {
                posX += srcBody.get(j).getX();
                posY += srcBody.get(j).getY();
            }
            if (prevPosX == posX) {
                for (int i = 0; i < Math.abs(srcBody.get(j).getY()); i++) {
                    if (srcBody.get(j).getY() < 0) newSnakeBody.add(new Cell(0, -1));
                    else newSnakeBody.add(new Cell(0 ,1));
                }
            }
            if (prevPosY == posY) {
                for (int i = 0; i < Math.abs(srcBody.get(j).getX()); i++) {
                    if (srcBody.get(j).getX() < 0) newSnakeBody.add(new Cell(-1, 0));
                    else newSnakeBody.add(new Cell(1 ,0));
                }
            }
            prevPosX = posX;
            prevPosY = posY;
        }
        return newSnakeBody;
    }
    private SnakesProto.GamePlayer getPlayerByID(int id, ArrayList<SnakesProto.GamePlayer> players) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId() == id) return players.get(i);
        }
        return null;
    }
    private  <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    public void calculateNextState() {
        ArrayList<Food> foodToRemove = new ArrayList<>();
        // Calculate next head position for each snake
        for (int i = 0; i < snakes.size(); i++) {
            Snake nextSnake = snakes.get(i);
            Cell head = new Cell(nextSnake.getBody().get(0));

            CollisionResolver.ResolverAnswer ans = collisionResolver.isNextSnakeStateIntersectsFood(head, nextSnake.getDirection(), getFoods());
            if (ans.isIntersects) {
                nextSnake.addPoint(nextSnake.getDirection());
                foodToRemove.add((Food) ans.whichIntersect);
                if (nextSnake.getSnakeState().equals(SnakeState.ALIVE))
                    gamePlayerMap.get(nextSnake).incrementScore();
            }
            else nextSnake.moveSnake(nextSnake.getDirection());
        }

        // Find out which snakes intersects each other
        ArrayList<Snake> snakesToRemove = new ArrayList<>();
        for (int i = 0; i < snakes.size(); i++) {
            Snake nextSnake = snakes.get(i);
            CollisionResolver.ResolverAnswer answer = collisionResolver.isSnakeIntersectsSnake(nextSnake, getSnakes());
            if (answer.isIntersects) {
                snakesToRemove.add(nextSnake);
                if (answer.whichIntersect != null &&
                        !((Snake)answer.whichIntersect).getSnakeState().equals(SnakeState.ZOMBIE)) gamePlayerMap.get((Snake) answer.whichIntersect).incrementScore();
            }
        }

        // Remove eaten food and intersected snakes
        for (int i = 0; i < snakesToRemove.size(); i++) {
            Snake nextSnake = snakesToRemove.get(i);
            GamePlayer lost = null;
            if (nextSnake.getSnakeState().equals(SnakeState.ALIVE)) {
                lost  = gamePlayerMap.get(nextSnake);
                gamePlayerMap.remove(nextSnake);
            }
            snakes.remove(nextSnake);
            replaceSnakeWithFood(nextSnake);
            if (nextSnake.getSnakeState().equals(SnakeState.ZOMBIE)) continue;
            Snake newSnake = spawnSnake();
            if (newSnake == null) {
                modelMain.sendErrorMessage("CANNOT FIND ENOUGH FREE SPACE FOR NEW SNAKE", gamePlayerMap.get(nextSnake));
                continue;
            }
            gamePlayerMap.put(newSnake, lost);
            lost.nullifySore();
            snakes.add(newSnake);
        }
        for (int i = 0; i < foodToRemove.size(); i++) {
            removeFood(foodToRemove.get(i));
            if (foods.size() < gameConfig.getFoodStatic() + snakes.size())
                spawnFood();
        }
    }
    private void removeFood(Food whichRemove) {
        Iterator<Food> iterator = foods.iterator();
        while (iterator.hasNext()) {
            Food elem = iterator.next();
            if (elem.equals(whichRemove)) {
                iterator.remove();
            }
        }
    }
    private Snake spawnSnake() {
        for (int i = 0; i < gameConfig.getWidth(); i += 5) {
            for (int j = 0; j < gameConfig.getHeight(); j += 5) {
                if (isFreeSquare(i, j, 5)) {
                    return new Snake(new Cell(i + 1, j + 1), Direction.LEFT, gameConfig.getWidth(), gameConfig.getHeight(), SnakeState.ALIVE);
                }
            }
        }
        return null;
    }
    private boolean isFreeSquare(int startX, int startY, int dimension) {
        Cell cell;
        for (int i = startX; i < startX + dimension; i++) {
            for (int j = startY; j < startY + dimension; j++) {
                cell = new Cell(i, j);
                for (int s = 0; s < snakes.size(); s++) {
                    Snake nextSnake = snakes.get(s);
                    if (collisionResolver.isFoodIntersectsSnake(new Food(cell), nextSnake)) return false;
                }
            }
        }
        return true;
    }
    private void spawnFood() {
        Random random = new Random();
        boolean spawned = false;
        Cell randFoodCell = null;
        while (! spawned) {
            randFoodCell = new Cell((random.nextInt() % gameConfig.getWidth() + gameConfig.getWidth()) % gameConfig.getWidth(),
                    (random.nextInt() % gameConfig.getHeight() + gameConfig.getHeight()) % gameConfig.getHeight());
            spawned = true;
            for (int i = 0; i < snakes.size(); i++) {
                if (collisionResolver.isFoodIntersectsSnake(new Food(randFoodCell), snakes.get(i))) {
                    i = snakes.size();
                    spawned = false;
                }
            }
        }
        foods.add(new Food(randFoodCell));
    }
    public GamePlayer getGamePlayer(Snake key) {
        return gamePlayerMap.get(key);
    }
    public ArrayList<Snake> getSnakes() {
        return new ArrayList<Snake>(snakes);
    }
    public ArrayList<Food> getFoods() {
        return new ArrayList<Food>(foods);
    }
    public GameConfig getGameConfig() {
        return new GameConfig(gameConfig);
    }
    public String getGameName() {
        return new String(gameName);
    }
    public void setSnakeDirection(GamePlayer gamePlayer) {
        Snake snake = getKeyByValue(gamePlayerMap, gamePlayer);
        if (snake.getSnakeState().equals(SnakeState.VIEWER)) return;
        if (snake == null) return;
        snake.setDirection(gamePlayer.getDirection());
    }
    public int addNewPlayer(GamePlayer player, NodeRole role) {
        ArrayList<GamePlayer> gamePlayers = new ArrayList<>(gamePlayerMap.values());
        for (GamePlayer p : gamePlayers) {
            if (p.getPort() == player.getPort() && p.getIpAddress().equals(player.getIpAddress())) return -1;
        }

        int ret = nextID;
        nextID++;
        player.setNodeRole(role);

        Snake newSnake = spawnSnake();
        if (newSnake == null) {
            modelMain.sendErrorMessage("CANNOT FIND ENOUGH FREE SPACE FOR NEW SNAKE", player);
            return -1;
        }
        gamePlayerMap.put(newSnake, player);
        snakes.add(newSnake);
        spawnFood();
        return ret;
    }
    public void replaceSnakeWithFood(Snake snake) {
        Random random = new Random();
        if (random.nextDouble() < 0.5) {
            int realX = 0;
            int realY = 0;
            for (int i = 0; i < snake.getBody().size(); i++) {
                realX += snake.getBody().get(i).x;
                realY += snake.getBody().get(i).y;
                foods.add(new Food(new Cell(realX, realY)));
            }
        }
    }
    public ArrayList<GamePlayer> getPlayers() {
        return new ArrayList<>(gamePlayerMap.values());
    }

    public void removePlayer(GamePlayer player) {
        Snake s = getKeyByValue(gamePlayerMap, player);
        if (s == null) {
            System.out.println("YOU TRY TO REMOVE NOT EXISTING PLAYER");
            return;
        }
        gamePlayerMap.remove(s);
        for (int i = 0; i < snakes.size(); i++) {
            if (snakes.get(i).equals(s)) {
                snakes.get(i).setSnakeState(SnakeState.ZOMBIE);
                return;
            }
        }
    }
}
