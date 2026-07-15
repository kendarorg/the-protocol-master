package org.kendar.amqp.v10.messages.sasl;

import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;

/**
 * AMQP 1.0 SASL {@code sasl-init} frame (frame type 1, descriptor 0x41).
 * <p>
 * The proxy terminates SASL (v09 credential-substitution model): it accepts the
 * client's {@code sasl-init} and replies {@code sasl-outcome(ok)}, while running
 * an independent SASL exchange upstream with the proxy's configured credentials.
 * The upstream exchange and outcome encoding are driven from {@code ProtocolHeader}
 * and completed with the M2 codec.
 */
public class SaslInit extends Amqp10BaseFrame {
    public SaslInit() {
        super();
    }

    public SaslInit(Class<?>... events) {
        super(events);
    }

    @Override
    protected byte expectedFrameType() {
        return FrameType.SASL.asByte();
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.SASL_INIT;
    }
}
