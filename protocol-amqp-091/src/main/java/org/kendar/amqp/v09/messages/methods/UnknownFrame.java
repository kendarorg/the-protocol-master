package org.kendar.amqp.v09.messages.methods;

import org.kendar.amqp.v09.messages.frames.Frame;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class UnknownFrame extends Frame {

    private static final Logger log = LoggerFactory.getLogger(UnknownFrame.class);

    public UnknownFrame(Class<?>... events) {
        super(events);
    }

    @Override
    protected void writeFrameContent(BBuffer rb) {

    }

    @Override
    protected boolean canRunFrame(BytesEvent event) {
        return true;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(short channel, BBuffer rb, BytesEvent event, int size) {

        var content = BBuffer.toHexByteArray(rb.getBytes(size));
        log.error("UNKNOWN FRAME " + getType() + content);
        throw new RuntimeException();
    }
}
