package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;
import org.kendar.protocol.states.InterruptProtoState;

/** AMQP 1.0 {@code detach} performative (link teardown, descriptor 0x16). */
public class Detach extends Amqp10BaseFrame implements InterruptProtoState {
    public Detach() {
        super();
    }

    public Detach(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.DETACH;
    }
}
