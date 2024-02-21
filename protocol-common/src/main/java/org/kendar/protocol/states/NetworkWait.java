package org.kendar.protocol.states;

import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.utils.Sleeper;

import java.util.Iterator;

public class NetworkWait extends ProtoState {
    public NetworkWait(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(BytesEvent event) {
        return event.getBuffer().size() == 0;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        Sleeper.sleep(10);
        return iteratorOfEmpty();
    }
}
