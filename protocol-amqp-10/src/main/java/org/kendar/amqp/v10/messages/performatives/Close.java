package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;
import org.kendar.protocol.states.InterruptProtoState;

/**
 * AMQP 1.0 {@code close} performative (connection teardown, descriptor 0x18).
 * Interrupt-capable so a client may close at any point (e.g. right after a
 * transfer, without ending the session first) without derailing the FSM.
 */
public class Close extends Amqp10BaseFrame implements InterruptProtoState {
    public Close() {
        super();
    }

    public Close(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.CLOSE;
    }
}
