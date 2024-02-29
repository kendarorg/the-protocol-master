package org.kendar.protocol.states;

import org.kendar.protocol.events.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special state in case of failure. May or may not carry to exception. Has its
 * own trace level logger
 */
public class FailedState extends ProtoState {
    private static final Logger log = LoggerFactory.getLogger(FailedState.class);
    private final String message;
    private ProtoState state;
    private BaseEvent event;

    public FailedState(String message) {
        this.message = message;
    }

    public FailedState(String message, ProtoState state, BaseEvent event) {
        log.trace("Failed state for event {}", event.getClass().getSimpleName());
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
