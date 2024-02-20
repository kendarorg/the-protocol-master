package org.kendar.amqp.v09.messages.methods;

import org.kendar.amqp.v09.messages.frames.MethodFrame;

public abstract class Basic extends MethodFrame {
    public Basic() {
        super();
    }

    public Basic(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 60);
        setMethod();
    }

    protected abstract void setMethod();
}
