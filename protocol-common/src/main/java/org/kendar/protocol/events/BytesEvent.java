package org.kendar.protocol.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;

/**
 * Default "bytes" kind event
 */
public class BytesEvent extends ProtocolEvent {
    private final BBuffer buffer;

    public BytesEvent(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState);
        this.buffer = buffer;
    }

    public BBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "BytesEvent{}";
    }
}
