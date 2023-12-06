package org.nsu.snake.model.components;

import org.nsu.snake.model.CollisionResolver;
import org.nsu.snake.model.GameConfig;
import org.w3c.dom.Node;

import java.util.*;

public class GameBoard {
    private GameConfig gameConfig;
    private List<Snake> snakes;
    private List<Food> foods;
    private Map<Snake, GamePlayer> gamePlayerMap;
    private Map<GamePlayer, NodeRole> gamePlayerNodeRoleMap;
    private int nextID = 1;
    private CollisionResolver collisionResolver;

    private String gameName;

    public GameBoard(GameConfig gameConfig, GamePlayer gamePlayer, NodeRole role, String gameName) {
        this.gameConfig = gameConfig;
        this.gameName = gameName;
        snakes = new ArrayList<>();
        foods = new ArrayList<>();
        collisionResolver = new CollisionResolver();

        Random random = new Random();
        Cell head = new Cell(Math.abs(random.nextInt() % (gameConfig.getWidth() - 1)), Math.abs(random.nextInt() % (gameConfig.getHeight() - 1)));
        Snake firstSnake = new Snake(head, Direction.LEFT, gameConfig.getWidth(), gameConfig.getHeight());
        snakes.add(firstSnake);

        gamePlayer.setID(nextID);
        nextID++;
        gamePlayer.setNodeRole(role);
        gamePlayerMap = new HashMap<>();
        gamePlayerMap.put(firstSnake, gamePlayer);

        gamePlayerNodeRoleMap = new HashMap<>();
        gamePlayerNodeRoleMap.put(gamePlayer, role);

        for (int i = 0; i < gameConfig.getFoodStatic() + 1; i++) {
            Cell randFoodCell = new Cell((random.nextInt() % gameConfig.getWidth() + gameConfig.getWidth()) % gameConfig.getWidth(),
                    (random.nextInt() % gameConfig.getHeight() + gameConfig.getHeight()) % gameConfig.getHeight());
            if (! collisionResolver.isFoodIntersectsSnake(new Food(randFoodCell), firstSnake)) {
                foods.add(new Food(randFoodCell));
            }
        }
    }

    private  <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    public void calculateNextState(Direction direction, GamePlayer gamePlayer) {
        Snake snake = getKeyByValue(gamePlayerMap, gamePlayer);
        assert snake != null;
        Cell head = new Cell(snake.getBody().get(0));

        if (collisionResolver.isNextSnakeStateIntersectsSnake(new Cell(head), direction, getSnakes())) {
            gamePlayerMap.remove(snake);
            snakes.remove(snake);
            Snake newSnake = spawnSnake();
            gamePlayerMap.put(newSnake, gamePlayer);
            snakes.add(newSnake);
            return;
        }

        CollisionResolver.ResolverAnswer ans = collisionResolver.isNextSnakeStateIntersectsFood(head, direction, getFoods());
        if (ans.isIntersects) {
            snake.addPoint(direction);
            removeFood( (Food) ans.whichIntersect);
            spawnFood();
            gamePlayer.incrementScore();
        }
        else snake.moveSnake(direction);
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
        Random random = new Random();
        boolean spawned = false;
        Cell randFoodCell_1 = null;
        Cell randFoodCell_2 = null;
        while (! spawned) {
            randFoodCell_1 = new Cell(random.nextInt() % gameConfig.getWidth(), random.nextInt() % gameConfig.getHeight());
            randFoodCell_2 = new Cell((randFoodCell_1.x + 1) % gameConfig.getWidth(), randFoodCell_1.y);
            spawned = true;
            for (int i = 0; i < snakes.size(); i++) {
                if ((collisionResolver.isFoodIntersectsSnake(new Food(randFoodCell_1), snakes.get(i)))
                || (collisionResolver.isFoodIntersectsSnake(new Food(randFoodCell_2), snakes.get(i)))) {
                    i = snakes.size();
                    spawned = false;
                }
            }
        }
        return new Snake(randFoodCell_1, Direction.LEFT, gameConfig.getWidth(), gameConfig.getHeight());
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
    public NodeRole getNodeRole(GamePlayer gamePlayer) {
        return gamePlayerNodeRoleMap.get(gamePlayer);
    }
}
