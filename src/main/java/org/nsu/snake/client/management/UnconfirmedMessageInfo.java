package org.nsu.snake.client.management;

import java.util.Objects;

public class UnconfirmedMessageInfo {
    public int destPort;
    public String destIP;
    public long messageSeq;
    public UnconfirmedMessageInfo(int destPort, String destIP, long messageSeq) {
        this.destIP = destIP;
        this.destPort = destPort;
        this.messageSeq = messageSeq;
    }
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UnconfirmedMessageInfo unconfirmedMessageInfo = (UnconfirmedMessageInfo) o;
        if (this.destPort == unconfirmedMessageInfo.destPort && Objects.equals(this.destIP, unconfirmedMessageInfo.destIP) &&
                this.messageSeq == unconfirmedMessageInfo.messageSeq) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(destPort, destIP, messageSeq);
    }
}
