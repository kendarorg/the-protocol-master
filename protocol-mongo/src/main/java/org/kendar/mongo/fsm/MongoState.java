package org.kendar.mongo.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.mongo.fsm.events.CompressedDataEvent;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

public abstract class MongoState extends ProtoState {
    public MongoState(Class<?>... events) {
        super(events);
    }

    public boolean canRun(CompressedDataEvent event) {
        return canRun(new BytesEvent(event.getContext(), event.getPrevState(), event.getBuffer()));
    }

    public boolean canRun(BytesEvent event) {
        var prevState = event.getPrevState();
        var inputBuffer = event.getBuffer();
        var pos = inputBuffer.getPosition();
        var canRun = false;
        if (inputBuffer.size() < 16) {
            canRun = false;
        } else {
            var length = inputBuffer.getInt(0);
            var opCode = OpCodes.of(inputBuffer.getInt(12));
            if(inputBuffer.size() >= length) {
                canRun =  opCode == getOpCode();
            }else{
                inputBuffer.setPosition(pos);
                throw new AskMoreDataException();
            }
        }
        return canRun;
    }

    protected abstract OpCodes getOpCode();

    public Iterator<ProtoStep> execute(CompressedDataEvent event) {
        return execute(new BytesEvent(event.getContext(), event.getPrevState(), event.getBuffer()));
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var inputBuffer = event.getBuffer();
        var protoContext = (MongoProtoContext) event.getContext();
        var length = inputBuffer.getInt(0);

        var buffer = protoContext.buildBuffer();
        inputBuffer.setPosition(0);
        buffer.write(inputBuffer.getBytes(length));
        buffer.setPosition(15);
        inputBuffer.truncate(length + 1);
        return executeStandardMessage(buffer, protoContext);
    }

    protected abstract Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, MongoProtoContext protoContext);
}
