package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.dtos.Binding;
import org.kendar.postgres.dtos.Parse;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;

public class Execute extends PostgresState {
    private static final Logger log = LoggerFactory.getLogger(Execute.class);
    private static int counter = 0;

    public Execute(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {
        return 'E';
    }

    @Override
    protected Iterator<ProtoStep> executeStandardMessage(BBuffer message, NetworkProtoContext protoContext) {
        counter++;
        var postgresContext = (PostgresProtoContext) protoContext;
        var portal = message.getString();
        var maxRecords = message.getInt();
        var bindMessage = (Binding) postgresContext.getValue("PORTAL_" + portal);

        if (bindMessage == null) {
            bindMessage = new Binding(null, null, new ArrayList<>(), new ArrayList<>());
        }
        var parseMessage = (Parse) postgresContext.getValue(bindMessage.getStatement());
        parseMessage.getBinds().remove("PORTAL_" + portal);
        var executor = postgresContext.getExecutor();


        log.debug("[SERVER][STMTEXEC]: Max:{} Query:{}", maxRecords, parseMessage.getQuery());
        var res = executor.executePortal(
                postgresContext, parseMessage, bindMessage, maxRecords,
                bindMessage.isDescribable() || parseMessage.isDescribable(),
                false);
        if (res.isRunNow()) {
            postgresContext.clearSync();
            return res.getReturnMessages();
        } else {
            postgresContext.addSync(res.getReturnMessages());
        }
        return iteratorOfEmpty();
    }


}
