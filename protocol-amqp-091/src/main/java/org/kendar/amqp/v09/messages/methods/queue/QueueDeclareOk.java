package org.kendar.amqp.v09.messages.methods.queue;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Queue;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class QueueDeclareOk extends Queue {

    private String queueName;
    private int messageCount;
    private int consumerCount;

    public QueueDeclareOk() {
        super();
    }

    public QueueDeclareOk(Class<?>... events) {
        super(events);
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getConsumerCount() {
        return consumerCount;
    }

    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 11);
    }


    @Override
    protected void writePreArguments(BBuffer rb) {

        new ShortStringHelper(queueName).write(rb);
        rb.writeInt(messageCount);
        rb.writeInt(consumerCount);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        this.setChannel(channel);
        this.queueName = ShortStringHelper.read(rb);
        this.messageCount = rb.getInt();
        this.consumerCount = rb.getInt();
        return iteratorOfList(this);
    }
}
