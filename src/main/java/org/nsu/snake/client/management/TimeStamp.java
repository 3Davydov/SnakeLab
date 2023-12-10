package org.nsu.snake.client.management;

import java.util.Objects;

public class TimeStamp {
    public Long timeSpot;
    public TimeStamp(Long timeSpot) {
        this.timeSpot = timeSpot;
    }
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TimeStamp timeStamp = (TimeStamp) o;
        if (this.timeSpot.longValue() == timeStamp.timeSpot.longValue()) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(timeSpot);
    }
}
