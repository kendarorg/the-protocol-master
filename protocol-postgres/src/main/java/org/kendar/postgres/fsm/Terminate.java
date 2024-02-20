package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.Stop;

import java.util.Iterator;

public class Terminate extends PostgresState {
    public Terminate(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {

        return 'X';
    }

    @Override
    public Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, NetworkProtoContext protoContext) {

        return iteratorOfList(new Stop());
    }
}
