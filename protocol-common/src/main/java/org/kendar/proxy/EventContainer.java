package org.kendar.proxy;

import org.kendar.protocol.events.ProtocolEvent;

public class EventContainer {
    private ProtocolEvent event;
    private int length;

    public EventContainer() {

    }

    public EventContainer(ProtocolEvent event, int size) {
        this.event = event;
        this.length = size;
    }

    public ProtocolEvent getEvent() {
        return event;
    }

    public void setEvent(ProtocolEvent event) {
        this.event = event;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
