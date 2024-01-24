package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.dtos.Binding;
import org.kendar.postgres.dtos.Parse;
import org.kendar.postgres.executor.PostgresExecutor;
import org.kendar.postgres.messages.ReadyForQuery;
import org.kendar.protocol.IteratorOfLists;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoStep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class Query extends StandardMessage {
    public Query(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {
        return 'Q';
    }

    @Override
    public Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, ProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        var query = inputBuffer.getUtf8String();
        var fakePortalStatement = UUID.randomUUID().toString();
        var executor = new PostgresExecutor();

        var bindMessage = new Binding("STATEMENT_" + fakePortalStatement, "PORTAL_" + fakePortalStatement, new ArrayList<>(), new ArrayList<>());
        var parseMessage = new Parse("STATEMENT_" + fakePortalStatement, query, new ArrayList<>(), new ArrayList<>());
        System.out.println("[SERVER] \tExecuting: " + parseMessage.getQuery());
        var res = executor.executePortal(
                protoContext, parseMessage, bindMessage, Integer.MAX_VALUE,
                true, true);
        var itol = new IteratorOfLists<ProtoStep>();
        itol.addIterator(res.getReturnMessages());
        itol.addIterator(iteratorOfList(new ReadyForQuery(protoContext.isTransaction())));
        postgresContext.clearSync();
        return itol;
    }
}
