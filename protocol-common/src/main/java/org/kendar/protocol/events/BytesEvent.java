package org.kendar.protocol.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;

public class BytesEvent extends BaseEvent {
    private final BBuffer buffer;

    public BytesEvent(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState);
        this.buffer = buffer;
    }

    public BBuffer getBuffer() {
        return buffer;
    }
}
