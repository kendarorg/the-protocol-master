package org.kendar.amqp.v09.messages.methods.queue;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Queue;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class QueuePurgeOk extends Queue {

    private int messageCount;

    public QueuePurgeOk() {
        super();
    }

    public QueuePurgeOk(Class<?>... events) {
        super(events);
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 31);
    }


    @Override
    protected void writePreArguments(BBuffer rb) {

        rb.writeInt(messageCount);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var result = new QueuePurgeOk();
        result.setMessageCount(rb.getInt());
        result.setChannel(channel);
        return iteratorOfList(result);
    }
}
