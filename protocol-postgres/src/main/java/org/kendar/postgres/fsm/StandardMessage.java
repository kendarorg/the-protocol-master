package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

public abstract class StandardMessage extends ProtoState {
    public StandardMessage(Class<?>... messages) {
        super(messages);
    }

    protected int getLength(BBuffer inputBuffer) {
        return inputBuffer.getInt(1);
    }


    public boolean canRun(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        if (inputBuffer.size() < 5 || inputBuffer.get(0) != getMessageId()) {
            return false;
        }
        var length = inputBuffer.getInt(1);
        return inputBuffer.size() >= length;
    }

    protected abstract byte getMessageId();


    public Iterator<ProtoStep> execute(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        var protoContext = event.getContext();
        inputBuffer.setPosition(5);
        var res = executeStandardMessage(inputBuffer, protoContext);
        var length = inputBuffer.getInt(1);
        inputBuffer.truncate(length + 1);
        return res;
    }

    protected abstract Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, ProtoContext protoContext);
}
