package org.kendar.protocol.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;

/**
 * Default "bytes" kind event
 */
public class BytesEvent extends BaseBytesEvent{

    public BytesEvent(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState,buffer);
    }
}
