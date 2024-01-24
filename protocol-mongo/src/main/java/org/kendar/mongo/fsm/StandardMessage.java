package org.kendar.mongo.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.mongo.fsm.events.CompressedDataEvent;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

public abstract class StandardMessage extends ProtoState {
    public StandardMessage(Class<?>... events) {
        super(events);
    }

    public boolean canRun(CompressedDataEvent event) {
        return canRun(new BytesEvent(event.getContext(), event.getPrevState(), event.getBuffer()));
    }

    public boolean canRun(BytesEvent event) {
        var prevState = event.getPrevState();
        var inputBuffer = event.getBuffer();
        var canRun = false;
        if (inputBuffer.size() < 16) {
            canRun = false;
        } else {
            var length = inputBuffer.getInt(0);
            var opCode = OpCodes.of(inputBuffer.getInt(12));
            canRun = inputBuffer.size() >= length && opCode == getOpCode();
        }
        return canRun;
    }

    protected abstract OpCodes getOpCode();

    public Iterator<ProtoStep> execute(CompressedDataEvent event) {
        return execute(new BytesEvent(event.getContext(), event.getPrevState(), event.getBuffer()));
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        var protoContext = event.getContext();
        var length = inputBuffer.getInt(0);

        var buffer = protoContext.buildBuffer();
        inputBuffer.setPosition(0);
        buffer.write(inputBuffer.getBytes(length));
        buffer.setPosition(15);
        inputBuffer.truncate(length + 1);
        return executeStandardMessage(buffer, protoContext);
    }

    protected abstract Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, ProtoContext protoContext);
}
