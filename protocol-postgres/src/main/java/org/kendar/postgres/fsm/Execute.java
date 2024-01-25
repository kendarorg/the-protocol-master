package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.dtos.Binding;
import org.kendar.postgres.dtos.Parse;
import org.kendar.postgres.executor.PostgresExecutor;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoStep;

import java.util.ArrayList;
import java.util.Iterator;

public class Execute extends StandardMessage {
    public Execute(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {
        return 'E';
    }

    @Override
    protected Iterator<ProtoStep> executeStandardMessage(BBuffer message, ProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        var portal = message.getString();
        var maxRecords = message.getInt();
        var bindMessage = (Binding) postgresContext.getValue("PORTAL_" + portal);
        Parse parseMessage = null;
        try {
            if (bindMessage == null) {
                bindMessage = new Binding(null, null, new ArrayList<>(), new ArrayList<>());
            }
            parseMessage = (Parse) postgresContext.getValue(bindMessage.getStatement());
            parseMessage.getBinds().remove("PORTAL_" + portal);
            var executor = new PostgresExecutor();

            System.out.println("[SERVER] \tExecuting: (" + maxRecords + ") " + parseMessage.getQuery());
            //for maxRecords
            var res = executor.executePortal(
                    protoContext, parseMessage, bindMessage, maxRecords,
                    bindMessage.isDescribable() || parseMessage.isDescribable(), false);
            if (res.isRunNow()) {
                postgresContext.clearSync();
                return res.getReturnMessages();
            } else {
                postgresContext.addSync(res.getReturnMessages());
            }
            return iteratorOfEmpty();
        } finally {
        }
    }


}
