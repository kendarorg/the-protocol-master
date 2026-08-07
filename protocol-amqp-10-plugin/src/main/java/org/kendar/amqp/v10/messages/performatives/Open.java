package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/** AMQP 1.0 {@code open} performative (connection-scoped, descriptor 0x10). */
public class Open extends Amqp10BaseFrame {
    public Open() {
        super();
    }

    public Open(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.OPEN;
    }
}
