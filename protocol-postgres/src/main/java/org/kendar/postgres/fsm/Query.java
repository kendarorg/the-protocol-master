package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.iterators.IteratorOfLists;
import org.kendar.postgres.dtos.Binding;
import org.kendar.postgres.dtos.Parse;
import org.kendar.postgres.executor.PostgresExecutor;
import org.kendar.postgres.messages.ReadyForQuery;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class Query extends PostgresState {
    private static final Logger log = LoggerFactory.getLogger(Query.class);

    public Query(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {
        return 'Q';
    }

    @Override
    public Iterator<ProtoStep> executeStandardMessage(BBuffer inputBuffer, NetworkProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        var query = inputBuffer.getUtf8String();
        var fakePortalStatement = UUID.randomUUID().toString();
        var executor = postgresContext.getExecutor();

        var bindMessage = new Binding("STATEMENT_" + fakePortalStatement, "PORTAL_" + fakePortalStatement, new ArrayList<>(), new ArrayList<>());
        var parseMessage = new Parse("STATEMENT_" + fakePortalStatement, query, new ArrayList<>(), new ArrayList<>());

        log.info("[SERVER][QUERY][1]: {}", parseMessage.getQuery());
        var res = executor.executePortal(
                postgresContext, parseMessage, bindMessage, Integer.MAX_VALUE,
                true, true);
        var itol = new IteratorOfLists<ProtoStep>();
        itol.addIterator(res.getReturnMessages());
        itol.addIterator(iteratorOfList(new ReadyForQuery(protoContext.getValue("TRANSACTION", false))));
        postgresContext.clearSync();
        return itol;
    }
}
