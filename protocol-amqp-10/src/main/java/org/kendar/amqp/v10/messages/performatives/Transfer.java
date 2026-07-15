package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/**
 * AMQP 1.0 {@code transfer} performative (message delivery, descriptor 0x14).
 * <p>
 * Must remain a single repeatable state (never a sequence): fragments of one
 * delivery ({@code more=true}) may interleave with transfers on other links of
 * the same session. Fragment reassembly by handle + delivery-id lands in M3.
 */
public class Transfer extends Amqp10BaseFrame {
    public Transfer() {
        super();
    }

    public Transfer(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.TRANSFER;
    }
}
