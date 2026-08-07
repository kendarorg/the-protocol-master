package org.kendar.amqp.v10.messages.sasl;

import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/**
 * SASL {@code sasl-mechanisms} frame (type 1, descriptor 0x40) the broker sends
 * to advertise its mechanisms. Relayed to the client (proxy direction).
 */
public class SaslMechanisms extends Amqp10BaseFrame {
    public SaslMechanisms() {
        super();
    }

    public SaslMechanisms(Class<?>... events) {
        super(events);
    }

    @Override
    protected byte expectedFrameType() {
        return FrameType.SASL.asByte();
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.SASL_MECHANISMS;
    }
}
