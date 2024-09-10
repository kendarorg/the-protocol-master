package org.kendar.amqp.v09.messages.methods.queue;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Queue;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class QueueUnbindOk extends Queue {


    public QueueUnbindOk() {
        super();
    }

    public QueueUnbindOk(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 51);
    }


    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var result = new QueueUnbindOk();
        result.setChannel(channel);
        return iteratorOfList(result);
    }
}
