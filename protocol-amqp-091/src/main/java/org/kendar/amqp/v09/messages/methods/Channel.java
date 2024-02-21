package org.kendar.amqp.v09.messages.methods;

import org.kendar.amqp.v09.messages.frames.MethodFrame;

public abstract class Channel extends MethodFrame {
    public Channel() {
        super();
    }

    public Channel(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 20);
        setMethod();
    }

    protected abstract void setMethod();
}
