package org.kendar.protocol.states;

import org.kendar.protocol.events.BaseEvent;

public class FailedState extends ProtoState {
    private final String message;
    private ProtoState state;
    private BaseEvent event;

    public FailedState(String message) {
        this.message = message;
    }

    public FailedState(String message, ProtoState state, BaseEvent event) {
        this.message = message;
        this.state = state;
        this.event = event;
    }

    public String getMessage() {
        return message;
    }

    public ProtoState getState() {
        return state;
    }

    public BaseEvent getEvent() {
        return event;
    }
}
