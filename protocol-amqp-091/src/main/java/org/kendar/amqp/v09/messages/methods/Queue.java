package org.kendar.amqp.v09.messages.methods;

import org.kendar.amqp.v09.messages.frames.MethodFrame;

public abstract class Queue extends MethodFrame {
    public Queue() {
        super();
    }

    public Queue(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 50);
        setMethod();
    }

    protected abstract void setMethod();
}
