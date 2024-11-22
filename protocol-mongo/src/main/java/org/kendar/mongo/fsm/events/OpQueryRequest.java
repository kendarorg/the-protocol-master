package org.kendar.mongo.fsm.events;

import org.kendar.mongo.dtos.OpQueryContent;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.ProtocolEvent;

public class OpQueryRequest extends ProtocolEvent {

    private final OpQueryContent data;

    public OpQueryRequest(ProtoContext context, Class<?> prevState, OpQueryContent data) {
        super(context, prevState);
        this.data = data;
    }

    public OpQueryContent getData() {
        return data;
    }

    @Override
    public String toString() {
        return "OpQueryRequest{}";
    }
}
