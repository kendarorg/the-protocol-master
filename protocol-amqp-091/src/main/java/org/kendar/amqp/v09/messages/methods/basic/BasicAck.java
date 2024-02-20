package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class BasicAck extends Basic {

    private long deliveryTag;
    private boolean multiple;

    public BasicAck() {
        super();
    }

    public BasicAck(Class<?>... events) {
        super(events);
    }

    public long getDeliveryTag() {
        return deliveryTag;
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 80);
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeLong(deliveryTag);
        rb.write((byte) (multiple ? 0x01 : 0x00));
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var toSend = new BasicAck();
        toSend.setChannel(channel);
        toSend.setDeliveryTag(rb.getLong());
        toSend.setMultiple(rb.get() > 0x00);

        return iteratorOfRunnable(() -> proxy.execute(context,
                connection,
                toSend
        ));
    }


}
