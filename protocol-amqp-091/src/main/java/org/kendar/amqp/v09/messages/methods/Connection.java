package org.kendar.amqp.v09.messages.methods;

import org.kendar.amqp.v09.messages.frames.MethodFrame;

public abstract class Connection extends MethodFrame {
    public Connection() {
        super();
    }

    public Connection(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 10);
        setMethod();
    }

    protected abstract void setMethod();
}
