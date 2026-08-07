package org.kendar.amqp.v10.messages.sasl;

import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/**
 * SASL {@code sasl-outcome} frame (type 1, descriptor 0x44) the broker sends with
 * the authentication result. Relayed to the client (proxy direction).
 */
public class SaslOutcome extends Amqp10BaseFrame {
    public SaslOutcome() {
        super();
    }

    public SaslOutcome(Class<?>... events) {
        super(events);
    }

    @Override
    protected byte expectedFrameType() {
        return FrameType.SASL.asByte();
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.SASL_OUTCOME;
    }
}
