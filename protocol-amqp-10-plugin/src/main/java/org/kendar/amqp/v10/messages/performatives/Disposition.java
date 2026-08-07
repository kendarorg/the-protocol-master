package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/** AMQP 1.0 {@code disposition} performative (delivery state, descriptor 0x15). */
public class Disposition extends Amqp10BaseFrame {
    public Disposition() {
        super();
    }

    public Disposition(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.DISPOSITION;
    }
}
