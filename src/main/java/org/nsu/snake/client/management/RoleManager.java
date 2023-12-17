package org.nsu.snake.client.management;

import org.nsu.snake.client.ClientMain;
import org.nsu.snake.client.view.ClientGUI;
import org.nsu.snake.model.components.GamePlayer;
import org.nsu.snake.model.components.NodeRole;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RoleManager {
    private ClientMain clientMain;
    private Map<TrackedNode, NodeRole> nodeRoleMap;

    public RoleManager(ClientMain clientMain) {
        this.clientMain = clientMain;
        nodeRoleMap = new HashMap<>();
    }
    public void refreshPlayerRoles(SnakesProto.GamePlayers players) {
        ArrayList<SnakesProto.GamePlayer> pList = new ArrayList<>(players.getPlayersList());
        for (SnakesProto.GamePlayer p : pList) {
            TrackedNode trackedNode = new TrackedNode(p.getPort(), p.getIpAddress());

            NodeRole role = NodeRole.NORMAL;
            switch (p.getRole()) {
                case VIEWER -> role = NodeRole.VIEWER;
                case MASTER -> role = NodeRole.MASTER;
                case DEPUTY -> role = NodeRole.DEPUTY;
                case NORMAL -> role = NodeRole.NORMAL;
            }

            if (nodeRoleMap.get(trackedNode) == null) {
                nodeRoleMap.put(trackedNode, role);
            }
            else if (! nodeRoleMap.get(trackedNode).equals(role)) {
                nodeRoleMap.remove(trackedNode);
                nodeRoleMap.put(trackedNode, role);
            }
        }
    }

    public NodeRole getPlayerRole(GamePlayer player) {
        if (player == null) return null;
        TrackedNode trackedNode = new TrackedNode(player.getPort(), player.getIpAddress());
        return nodeRoleMap.get(trackedNode);
    }

    public NodeRole getPlayerRole(TrackedNode player) {
        return nodeRoleMap.get(player);
    }

    public void addPlayer(GamePlayer player, NodeRole role) {
        TrackedNode trackedNode = new TrackedNode(player.getPort(), player.getIpAddress());
        nodeRoleMap.remove(trackedNode);
        nodeRoleMap.put(trackedNode, role);
    }

    public void addPlayer(TrackedNode player, NodeRole role) {
        nodeRoleMap.remove(player);
        nodeRoleMap.put(player, role);
    }
    public void removePlayer(GamePlayer player) {
        TrackedNode trackedNode = new TrackedNode(player.getPort(), player.getIpAddress());
        nodeRoleMap.remove(trackedNode);
    }

    public void removePlayer(TrackedNode node) {
        nodeRoleMap.remove(node);
    }

    public TrackedNode getPlayerWithRole(NodeRole role) {
        return getKeyByValue(nodeRoleMap, role);
    }

    public void clearAll() {
        nodeRoleMap.clear();
    }
    private  <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
