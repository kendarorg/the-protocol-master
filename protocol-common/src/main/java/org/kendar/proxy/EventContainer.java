package org.kendar.proxy;

import org.kendar.protocol.events.BaseEvent;

public class EventContainer {
    private BaseEvent event;
    private int length;
    public EventContainer() {

    }

    public EventContainer(BaseEvent event, int size) {
        this.event = event;
        this.length = size;
    }

    public BaseEvent getEvent() {
        return event;
    }

    public void setEvent(BaseEvent event) {
        this.event = event;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
