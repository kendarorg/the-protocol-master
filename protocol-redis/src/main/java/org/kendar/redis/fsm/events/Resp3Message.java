package org.kendar.redis.fsm.events;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.states.TaggedObject;

import java.util.List;

public class Resp3Message extends BaseEvent implements TaggedObject {
    private final List<Object> data;

    public Resp3Message(ProtoContext context, Class<?> prevState, List<Object> data) {
        super(context, prevState);
        this.data = data;
    }


    public List<Object> getData() {
        return data;
    }
}

