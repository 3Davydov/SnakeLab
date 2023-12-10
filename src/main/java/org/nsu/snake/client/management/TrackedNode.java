package org.nsu.snake.client.management;

import java.util.Objects;

public class TrackedNode {
    public int port;
    public String ip;

    public TrackedNode(int port, String ip) {
        this.port = port;
        this.ip = ip;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TrackedNode trackedNode = (TrackedNode) o;
        if (this.port == trackedNode.port && Objects.equals(this.ip, trackedNode.ip)) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(port, ip);
    }
}
