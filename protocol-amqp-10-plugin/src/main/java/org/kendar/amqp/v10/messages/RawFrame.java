package org.kendar.amqp.v10.messages;

/**
 * A verbatim AMQP 1.0 frame used purely as an outbound message (never an FSM
 * state): it writes its stored raw bytes unchanged. Used for M1 passthrough.
 */
public class RawFrame extends Amqp10BaseFrame {
    private final long descriptorCode;
    private final byte frameKind;

    public RawFrame(long descriptorCode, byte frameKind) {
        super();
        this.descriptorCode = descriptorCode;
        this.frameKind = frameKind;
        setFrameType(frameKind);
    }

    @Override
    protected long getDescriptorCode() {
        return descriptorCode;
    }

    @Override
    protected byte expectedFrameType() {
        return frameKind;
    }
}
