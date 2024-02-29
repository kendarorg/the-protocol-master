package org.kendar.amqp.v09.messages.methods;

import org.kendar.amqp.v09.messages.frames.MethodFrame;

public abstract class Exchange extends MethodFrame {
    public Exchange() {
        super();
    }

    public Exchange(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 40);
        setMethod();
    }

    protected abstract void setMethod();
}
