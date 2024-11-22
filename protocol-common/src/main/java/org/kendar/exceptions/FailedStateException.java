package org.kendar.exceptions;

import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.states.NullState;
import org.kendar.protocol.states.ProtoState;

public class FailedStateException extends RuntimeException {
    private final String message;
    private final ProtoState state;
    private final ProtocolEvent event;

    public FailedStateException(String message, ProtoState state, ProtocolEvent event) {

        this.message = message;
        this.state = state;
        this.event = event;
    }

    public FailedStateException(String message, ProtocolEvent event) {

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

    public ProtocolEvent getEvent() {
        return event;
    }
}
