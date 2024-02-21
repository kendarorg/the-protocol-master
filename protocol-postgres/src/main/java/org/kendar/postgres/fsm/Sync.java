package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.messages.ReadyForQuery;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class Sync extends PostgresState {
    public Sync(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {
        return 'S';
    }

    @Override
    protected Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, NetworkProtoContext protoContext) {

        var postgresContext = (PostgresProtoContext) protoContext;
        postgresContext.addSync(iteratorOfList(new ReadyForQuery(protoContext.isTransaction())));
        return postgresContext.clearSync();
    }
}
