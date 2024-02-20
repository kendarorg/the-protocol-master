package org.kendar.postgres.fsm;

import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public class ConnectionEstablished extends ProtoState {
    public ConnectionEstablished(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        return inputBuffer.size() == 0;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        return iteratorOfEmpty();
    }

}
