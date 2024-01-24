package org.kendar.mongo.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.fsm.BaseEvent;

public class CompressedDataEvent extends BaseEvent {
    private final BBuffer buffer;

    public CompressedDataEvent(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState);
        this.buffer = buffer;
    }

    public BBuffer getBuffer() {
        return buffer;
    }
}
