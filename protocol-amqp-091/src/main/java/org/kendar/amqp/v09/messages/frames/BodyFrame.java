package org.kendar.amqp.v09.messages.frames;

import org.kendar.amqp.v09.dtos.FrameType;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;

public class BodyFrame extends Frame{
    public BodyFrame() {
        setType(FrameType.BODY.asByte());
    }

    @Override
    protected void writeFrameContent(BBuffer rb) {

    }

    @Override
    protected boolean canRunFrame(BytesEvent event) {
        return false;
    }

    @Override
    protected Iterator<ProtoStep> executeFrame(short channel, BBuffer rb, BytesEvent event) {
        return null;
    }
}
