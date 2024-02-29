package org.kendar.postgres.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.BaseEvent;

public class PostgresPacket extends BaseEvent {
    private final BBuffer buffer;

    public PostgresPacket(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState);
        this.buffer = buffer;
    }

    public BBuffer getBuffer() {
        return buffer;
    }
}
