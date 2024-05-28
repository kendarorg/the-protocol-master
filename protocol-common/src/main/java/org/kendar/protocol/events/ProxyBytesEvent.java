package org.kendar.protocol.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;

/**
 * Default "bytes" kind event
 */
public class ProxyBytesEvent extends BaseEvent {
    private final BBuffer buffer;

    public ProxyBytesEvent(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState);
        this.buffer = buffer;
    }

    public BBuffer getBuffer() {
        return buffer;
    }
}
