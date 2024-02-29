package org.kendar.exceptions;

import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.states.NullState;
import org.kendar.protocol.states.ProtoState;

public class FailedStateException extends RuntimeException {
    private final String message;
    private final ProtoState state;
    private final BaseEvent event;

    public FailedStateException(String message, ProtoState state, BaseEvent event) {

        this.message = message;
        this.state = state;
        this.event = event;
    }

    public FailedStateException(String message, BaseEvent event) {

        this.message = message;
        this.state = new NullState();
        this.event = event;
    }

    @Override
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
