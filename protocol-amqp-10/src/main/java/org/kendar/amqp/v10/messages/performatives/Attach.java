package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/** AMQP 1.0 {@code attach} performative (link setup, descriptor 0x12). */
public class Attach extends Amqp10BaseFrame {
    public Attach() {
        super();
    }

    public Attach(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.ATTACH;
    }
}
