package org.kendar.amqp.v09.messages.methods.queue;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Queue;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class QueueDeleteOk extends Queue {

    private int messageCount;

    public QueueDeleteOk() {
        super();
    }

    public QueueDeleteOk(Class<?>... events) {
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
        setMethodId((short) 41);
    }


    @Override
    protected void writePreArguments(BBuffer rb) {

        rb.writeInt(messageCount);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var result = new QueueDeleteOk();
        result.setChannel(channel);
        result.setMessageCount(rb.getInt());
        return iteratorOfList(result);
    }
}
