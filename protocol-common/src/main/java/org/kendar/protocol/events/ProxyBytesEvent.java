package org.kendar.protocol.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;

/**
 * Default "bytes" kind event
 */
public class ProxyBytesEvent extends BaseBytesEvent{

    public ProxyBytesEvent(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState,buffer);
    }
}
