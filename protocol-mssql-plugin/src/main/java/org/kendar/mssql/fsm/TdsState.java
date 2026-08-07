package org.kendar.mssql.fsm;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public abstract class TdsState extends ProtoState {
    public TdsState(Class<?>... messages) {
        super(messages);
    }

    protected abstract byte getPacketType();

    public boolean canRun(TdsPacket event) {
        return event.getPacketType() == getPacketType();
    }

    public Iterator<ProtoStep> execute(TdsPacket event) {
        var inputBuffer = event.getBuffer();
        inputBuffer.setPosition(0);
        return executeTds(inputBuffer, (MssqlProtoContext) event.getContext(), event);
    }

    protected abstract Iterator<ProtoStep> executeTds(MssqlBBuffer inputBuffer, MssqlProtoContext protoContext, TdsPacket event);

    /**
     * Skip the ALL_HEADERS structure (leading DWORD with its total length,
     * the length includes the DWORD itself)
     */
    protected void skipAllHeaders(MssqlBBuffer inputBuffer) {
        if (inputBuffer.size() - inputBuffer.getPosition() < 4) {
            return;
        }
        var start = Math.max(inputBuffer.getPosition(), 0);
        var totalLength = inputBuffer.readUIntLE();
        if (totalLength >= 4 && (start + totalLength) <= inputBuffer.size()) {
            inputBuffer.setPosition((int) (start + totalLength));
        } else {
            inputBuffer.setPosition(start);
        }
    }
}
