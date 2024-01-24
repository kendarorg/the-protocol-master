package org.kendar.protocol;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.fsm.BaseEvent;

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
