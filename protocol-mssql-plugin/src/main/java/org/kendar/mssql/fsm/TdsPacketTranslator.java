package org.kendar.mssql.fsm;

import org.kendar.exceptions.AskMoreDataException;
import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.executor.MssqlProtoContext;
import org.kendar.mssql.fsm.events.TdsPacket;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

/**
 * Reassembles the TDS packets: 8 bytes header (Type, Status, Length BE,
 * SPID BE, PacketID, Window), messages can span multiple packets and are
 * complete when a packet has the EOM status bit set
 */
public class TdsPacketTranslator extends ProtoState implements InterruptProtoState {
    private static final int HEADER_SIZE = 8;
    private static final int STATUS_EOM = 0x01;

    public TdsPacketTranslator(Class<?>... messages) {
        super(messages);
    }

    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        if (inputBuffer.size() == 0) {
            return false;
        }
        if (inputBuffer.size() < HEADER_SIZE) {
            throw new AskMoreDataException();
        }
        var offset = 0;
        while (true) {
            if ((offset + HEADER_SIZE) > inputBuffer.size()) {
                throw new AskMoreDataException();
            }
            var length = ((inputBuffer.get(offset + 2) & 0xFF) << 8) | (inputBuffer.get(offset + 3) & 0xFF);
            if (length < HEADER_SIZE) {
                return false;
            }
            if ((offset + length) > inputBuffer.size()) {
                throw new AskMoreDataException();
            }
            var status = inputBuffer.get(offset + 1);
            if ((status & STATUS_EOM) == STATUS_EOM) {
                return true;
            }
            offset += length;
        }
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        var context = (MssqlProtoContext) event.getContext();
        var packetType = inputBuffer.get(0);
        var payload = (MssqlBBuffer) context.buildBuffer();
        var offset = 0;
        while (true) {
            var length = ((inputBuffer.get(offset + 2) & 0xFF) << 8) | (inputBuffer.get(offset + 3) & 0xFF);
            var status = inputBuffer.get(offset + 1);
            payload.write(inputBuffer.getBytes(offset + HEADER_SIZE, length - HEADER_SIZE));
            offset += length;
            if ((status & STATUS_EOM) == STATUS_EOM) {
                break;
            }
        }
        inputBuffer.truncate(offset);
        payload.setPosition(0);
        context.send(new TdsPacket(context, event.getPrevState(), payload, packetType));
        return iteratorOfEmpty();
    }
}
