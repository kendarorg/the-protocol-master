package org.kendar.postgres.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.postgres.dtos.Binding;
import org.kendar.postgres.dtos.Parse;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class Describe extends PostgresState {
    public Describe(Class<?>... messages) {
        super(messages);
    }

    @Override
    protected byte getMessageId() {
        return 'D';
    }

    @Override
    protected Iterator<ProtoStep> executeStandardMessage(BBuffer message, NetworkProtoContext protoContext) {
        var postgresContext = (PostgresProtoContext) protoContext;
        var type = message.get();
        var name = message.getString();
        if (type == 'S') {
            var parseMessage = (Parse) postgresContext.getValue("STATEMENT" + name);
            parseMessage.setDescribable(true);
        } else {
            var bindMessage = (Binding) postgresContext.getValue("PORTAL_" + name);
            bindMessage.setDescribable(true);
        }

        return iteratorOfEmpty();
    }
}
