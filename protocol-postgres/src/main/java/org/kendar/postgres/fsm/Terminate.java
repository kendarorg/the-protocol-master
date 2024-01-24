package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.fsm.Stop;

import java.util.Iterator;

public class Terminate extends StandardMessage {
    public Terminate(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {
        return 'X';
    }

    @Override
    public Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, ProtoContext protoContext) {

        return iteratorOfList(new Stop());
    }
}
