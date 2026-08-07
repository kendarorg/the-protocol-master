package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/** AMQP 1.0 {@code flow} performative (credit-based flow control, descriptor 0x13). */
public class Flow extends Amqp10BaseFrame {
    public Flow() {
        super();
    }

    public Flow(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.FLOW;
    }
}
