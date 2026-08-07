package org.kendar.mssql.fsm.events;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.ProtocolEvent;

/**
 * A fully reassembled TDS message (headers stripped, payloads of all
 * the spanned packets concatenated)
 */
public class TdsPacket extends ProtocolEvent {
    private final MssqlBBuffer buffer;
    private final byte packetType;

    public TdsPacket(ProtoContext context, Class<?> prevState, MssqlBBuffer buffer, byte packetType) {
        super(context, prevState);
        this.buffer = buffer;
        this.packetType = packetType;
    }

    public MssqlBBuffer getBuffer() {
        return buffer;
    }

    public byte getPacketType() {
        return packetType;
    }

    @Override
    public String toString() {
        return "TdsPacket{type=" + packetType + "}";
    }
}
