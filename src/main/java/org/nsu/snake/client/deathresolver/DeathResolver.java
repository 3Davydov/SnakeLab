package org.nsu.snake.client.deathresolver;

import org.nsu.snake.client.ClientMain;
import org.nsu.snake.model.components.NodeRole;

public class DeathResolver {
    private ClientMain clientMain;
    public DeathResolver(ClientMain clientMain) {
        this.clientMain = clientMain;
    }
    public void resolveNodeDeath(NodeRole deadNodeRole, NodeRole clientNodeRole) {
        if (deadNodeRole.equals(NodeRole.MASTER) && clientNodeRole.equals(NodeRole.DEPUTY))
            newKing();
    }
    private void newKing() {

    }
}
