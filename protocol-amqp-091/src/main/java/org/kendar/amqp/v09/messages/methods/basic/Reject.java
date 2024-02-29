package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class Reject extends Basic {

    private long deliveryTag;
    private boolean requeue;

    public Reject() {
        super();
    }

    public Reject(Class<?>... events) {
        super(events);
    }

    public boolean isRequeue() {
        return requeue;
    }

    public void setRequeue(boolean requeue) {
        this.requeue = requeue;
    }

    public long getDeliveryTag() {
        return deliveryTag;
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 90);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeLong(deliveryTag);
        rb.write((byte) (requeue ? 0x01 : 0x00));
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var toSend = new Reject();
        toSend.setChannel(channel);
        toSend.setDeliveryTag(rb.getLong());
        toSend.setRequeue(rb.get() > 0);

        return iteratorOfRunnable(() -> proxy.execute(context,
                connection,
                toSend
        ));
    }


}
