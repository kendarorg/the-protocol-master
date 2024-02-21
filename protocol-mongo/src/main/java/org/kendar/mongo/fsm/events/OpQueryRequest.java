package org.kendar.mongo.fsm.events;

import org.kendar.mongo.dtos.OpQueryContent;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.BaseEvent;

public class OpQueryRequest extends BaseEvent {

    private final OpQueryContent data;

    public OpQueryRequest(ProtoContext context, Class<?> prevState, OpQueryContent data) {
        super(context, prevState);
        this.data = data;
    }

    public OpQueryContent getData() {
        return data;
    }
}
