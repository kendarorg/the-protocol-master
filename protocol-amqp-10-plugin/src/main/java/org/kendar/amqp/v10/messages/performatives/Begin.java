package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/** AMQP 1.0 {@code begin} performative (session-scoped, descriptor 0x11). */
public class Begin extends Amqp10BaseFrame {
    public Begin() {
        super();
    }

    public Begin(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.BEGIN;
    }
}
