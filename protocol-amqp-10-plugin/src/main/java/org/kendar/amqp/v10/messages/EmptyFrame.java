package org.kendar.amqp.v10.messages;

import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.amqp.v10.fsm.events.Amqp10Frame;
import org.kendar.protocol.states.InterruptProtoState;

/**
 * The AMQP 1.0 heartbeat: an 8-byte frame with an empty body (size=8, doff=2,
 * type=0, channel=0). Replaces the v09 {@code HearthBeatFrame}; registered as an
 * interrupt state so it can arrive at any point.
 */
public class EmptyFrame extends Amqp10BaseFrame implements InterruptProtoState {

    public EmptyFrame() {
        super();
    }

    public EmptyFrame(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return -1;
    }

    @Override
    public boolean canRun(Amqp10Frame event) {
        if (event.getFrameType() != FrameType.AMQP.asByte()) {
            return false;
        }
        var rb = event.getBuffer();
        var pos = rb.getPosition();
        try {
            rb.setPosition(0);
            if (rb.size() < 8) {
                return false;
            }
            var size = rb.getInt();
            return size == 8; // empty body
        } finally {
            rb.setPosition(pos);
        }
    }
}
