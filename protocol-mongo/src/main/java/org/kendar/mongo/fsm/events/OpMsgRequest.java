package org.kendar.mongo.fsm.events;

import org.kendar.mongo.dtos.OpMsgContent;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.BaseEvent;

public class OpMsgRequest extends BaseEvent {
    private final OpMsgContent data;

    public OpMsgRequest(ProtoContext context, Class<?> prevState, OpMsgContent data) {
        super(context, prevState);
        this.data = data;
    }

    public OpMsgContent getData() {
        return data;
    }

    @Override
    public String toString() {
        return "OpMsgRequest{}";
    }
}
