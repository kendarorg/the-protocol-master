package org.kendar.amqp.v09.messages.methods;

import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class UnknownMethod extends MethodFrame {
    private static final Logger log = LoggerFactory.getLogger(UnknownMethod.class);

    protected boolean canRunFrame(BytesEvent event) {
        var rb = event.getBuffer();
        var pos = rb.getPosition();
        var classId = rb.getShort();
        var methodId = rb.getShort();
        rb.setPosition(pos);
        return true;
    }

    @Override
    protected void setClassAndMethod() {

    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {

        var size = Math.min(rb.size(), 20);

        var content = BBuffer.toHexByteArray(rb.getBytes(size));
        log.error("UNKNOWN METHOD " + classId + ":" + methodId + " " + content);
        throw new RuntimeException();
    }
}
