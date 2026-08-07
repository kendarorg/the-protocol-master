package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;
import org.kendar.protocol.states.InterruptProtoState;

/** AMQP 1.0 {@code end} performative (session teardown, descriptor 0x17). */
public class End extends Amqp10BaseFrame implements InterruptProtoState {
    public End() {
        super();
    }

    public End(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.END;
    }
}
