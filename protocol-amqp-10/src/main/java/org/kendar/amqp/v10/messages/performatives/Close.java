package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/** AMQP 1.0 {@code close} performative (connection teardown, descriptor 0x18). */
public class Close extends Amqp10BaseFrame {
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
