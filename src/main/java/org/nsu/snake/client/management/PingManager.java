package org.nsu.snake.client.management;

import org.nsu.snake.client.ClientMain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PingManager extends Thread {
    private Map<TrackedNode, TimeStamp> pingMap;
    private Map<TrackedNode, TimeStamp> timeoutMap;
    private ClientMain clientMain;
    private long delay;
    public PingManager(ClientMain clientMain, long delay) {
        this.clientMain = clientMain;
        this.delay = delay;
        this.pingMap = new ConcurrentHashMap<>();
        this.timeoutMap = new ConcurrentHashMap<>();
        start();
    }

    synchronized public void addTrackedNode(TrackedNode newNode) {
        TimeStamp timeStamp = new TimeStamp(System.currentTimeMillis());
        pingMap.put(newNode, timeStamp);
        timeoutMap.put(newNode, timeStamp);
    }

    synchronized public void rebootTrackedNode_send(TrackedNode node) {
        pingMap.remove(node);
        pingMap.put(node, new TimeStamp(System.currentTimeMillis()));
    }

    synchronized public void rebootTrackedNode_receive(TrackedNode node) {
        timeoutMap.remove(node);
        timeoutMap.put(node, new TimeStamp(System.currentTimeMillis()));
    }

    synchronized public void removeTrackedNode(TrackedNode node) {
        pingMap.remove(node);
        timeoutMap.remove(node);
    }

    @Override
    public void run() {
        while (! this.isInterrupted()) {
            try {
                if (pingMap.isEmpty()) {
                    sleep(delay);
                    continue;
                }
                ArrayList<TrackedNode> nodesToTrack = new ArrayList<>(pingMap.keySet());
                for (TrackedNode n : nodesToTrack) {
                    long timeSpot = 0;
                    if (pingMap.get(n) != null)
                        timeSpot = pingMap.get(n).timeSpot;
                    if (timeoutMap.get(n) != null && (System.currentTimeMillis() - timeoutMap.get(n).timeSpot > (delay * 8))) {
                        removeTrackedNode(n);
                        clientMain.processNodeDeath(n);
                    }
                    if (System.currentTimeMillis() - timeSpot > delay) {
                        clientMain.sendPingMessage(n.port, n.ip);
                        pingMap.remove(n);
                        pingMap.put(n, new TimeStamp(System.currentTimeMillis()));
                    }
                }
                sleep(delay);
            } catch (InterruptedException | IOException e) {
                this.interrupt();
                return;
            }
        }
    }
}
