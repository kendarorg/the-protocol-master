package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class BasicGetOk extends Basic {

    private long deliveryTag;
    private int messageCount;
    private boolean redelivered;
    private String exchange;
    private String routingKey;

    public BasicGetOk() {
        super();
    }

    public BasicGetOk(Class<?>... events) {
        super(events);
    }

    public long getDeliveryTag() {
        return deliveryTag;
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public boolean isRedelivered() {
        return redelivered;
    }

    public void setRedelivered(boolean redelivered) {
        this.redelivered = redelivered;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }


    @Override
    protected void setMethod() {
        setMethodId((short) 71);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeLong(deliveryTag);
        rb.write((byte) (redelivered ? 0x01 : 0x02));
        new ShortStringHelper(exchange).write(rb);
        new ShortStringHelper(routingKey).write(rb);
        rb.writeInt(messageCount);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        AmqpProxy proxy = (AmqpProxy) context.getProxy();
        ProxyConnection connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));


        var bd = new BasicGetOk();
        bd.setChannel(channel);
        bd.deliveryTag = rb.getLong();
        bd.redelivered = rb.get() > 0;
        bd.exchange = ShortStringHelper.read(rb);
        bd.routingKey = ShortStringHelper.read(rb);
        bd.messageCount = rb.getInt();


        return iteratorOfList(this);
    }
}
