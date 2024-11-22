package org.kendar.postgres.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.ProtocolEvent;

public class PostgresPacket extends ProtocolEvent {
    private final BBuffer buffer;

    public PostgresPacket(ProtoContext context, Class<?> prevState, BBuffer buffer) {
        super(context, prevState);
        this.buffer = buffer;
    }

    public BBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "PostgresPacket{}";
    }
}
