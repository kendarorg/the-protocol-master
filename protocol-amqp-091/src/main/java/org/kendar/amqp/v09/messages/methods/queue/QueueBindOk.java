package org.kendar.amqp.v09.messages.methods.queue;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Queue;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class QueueBindOk extends Queue {


    public QueueBindOk() {
        super();
    }

    public QueueBindOk(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 21);
    }


    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        setChannel(channel);
        return iteratorOfList(this);
    }
}
