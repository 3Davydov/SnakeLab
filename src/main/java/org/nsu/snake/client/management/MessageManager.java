package org.nsu.snake.client.management;

import org.nsu.snake.client.ClientMain;
import org.nsu.snake.proto.compiled_proto.SnakesProto;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManager extends Thread {
    private Map<TimeStamp, SnakesProto.GameMessage> unconfirmedMessagesMap;
    private Map<UnconfirmedMessageInfo, TimeStamp> messageInfoLongMap;
    private long resendDelay;
    private ClientMain clientMain;
    public MessageManager(long resendDelay, ClientMain clientMain) {
        this.unconfirmedMessagesMap = new ConcurrentHashMap<>();
        this.messageInfoLongMap = new ConcurrentHashMap<>();
        this.resendDelay = resendDelay;
        this.clientMain = clientMain;
        start();
    }
    synchronized public void addMessageToConfirmList(UnconfirmedMessageInfo messageInfo, SnakesProto.GameMessage message) {
        TimeStamp timeStamp = new TimeStamp(System.currentTimeMillis());
        unconfirmedMessagesMap.put(timeStamp, message);
        messageInfoLongMap.put(messageInfo, timeStamp);
    }
    synchronized public void removeMessageFromConfirmList(UnconfirmedMessageInfo messageInfo) {
        TimeStamp timeStamp = messageInfoLongMap.get(messageInfo);
        if (timeStamp == null) return;
        unconfirmedMessagesMap.remove(timeStamp);
        messageInfoLongMap.remove(messageInfo);
//        System.out.println("REMOVED BY CLIENT ");
    }

    @Override
    public void run() {
        while (! this.isInterrupted()) {
            try {
                if (unconfirmedMessagesMap.isEmpty()) {
                    sleep(resendDelay);
                    continue;
                }
                ArrayList<TimeStamp> unconfirmedList = new ArrayList<>(unconfirmedMessagesMap.keySet());
                for (TimeStamp timeStamp : unconfirmedList) {
                    if ((System.currentTimeMillis() - timeStamp.timeSpot) > resendDelay) {
                        if ((System.currentTimeMillis() - timeStamp.timeSpot) > (resendDelay * 10)) {
                            unconfirmedMessagesMap.remove(timeStamp);
                            if (getKeyByValue(messageInfoLongMap, timeStamp) != null)
                                messageInfoLongMap.remove(getKeyByValue(messageInfoLongMap, timeStamp));
//                            System.out.println("REMOVED BY TIMEOUT");
                            continue;
                        }
                        try {
                            UnconfirmedMessageInfo unconfirmedMessageInfo = getKeyByValue(messageInfoLongMap, timeStamp);
                            if (unconfirmedMessageInfo != null) {
//                                System.out.println("SENT CONFIRM " + unconfirmedMessagesMap.get(timeStamp).getTypeCase() + " " + unconfirmedMessagesMap.get(timeStamp).getMsgSeq());
                                clientMain.clientSocket.sendUnicastMessage(unconfirmedMessageInfo.destIP,
                                        unconfirmedMessageInfo.destPort, unconfirmedMessagesMap.get(timeStamp));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                sleep(resendDelay);
            } catch (InterruptedException e) {
                this.interrupt();
                return;
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
}
