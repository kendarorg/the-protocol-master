package org.kendar.redis.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.states.TaggedObject;

public class Resp3Message extends BaseEvent implements TaggedObject, NetworkReturnMessage {
    private final Object data;

    public String getMessage() {
        return message;
    }

    private final String message;

    public Resp3Message(ProtoContext context, Class<?> prevState, Object data,String message) {
        super(context, prevState);
        this.data = data;
        this.message = message;
    }


    public Object getData() {
        return data;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        try {
            var data = message.getBytes("ASCII");
            resultBuffer.write(data);
        }catch (Exception ex){
            System.err.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAARRRH");
        }
    }
}

