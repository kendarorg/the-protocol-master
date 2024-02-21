package org.kendar.protocol.events;

import org.kendar.protocol.context.ProtoContext;

public class BaseEvent {
    private final ProtoContext context;
    private final Class<?> prevState;

    public BaseEvent(ProtoContext context, Class<?> prevState) {

        this.context = context;
        this.prevState = prevState;
    }

    public ProtoContext getContext() {
        return context;
    }

    public Class<?> getPrevState() {
        return prevState;
    }
}
