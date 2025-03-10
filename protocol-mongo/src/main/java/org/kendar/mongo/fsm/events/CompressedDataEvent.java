package org.kendar.mongo.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.ProtocolEvent;

public class CompressedDataEvent extends ProtocolEvent {
    private final BBuffer buffer;

    public CompressedDataEvent(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState);
        this.buffer = buffer;
    }

    public BBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "CompressedDataEvent{}";
    }
}
