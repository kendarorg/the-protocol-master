package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferUtils;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;


public class BasicGet extends Basic {
    private String queue;
    private short reserved1;
    private boolean noAck;

    public BasicGet() {
        super();
    }

    public BasicGet(Class<?>... events) {
        super(events);
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public short getReserved1() {
        return reserved1;
    }

    public void setReserved1(short reserved1) {
        this.reserved1 = reserved1;
    }

    public boolean isNoAck() {
        return noAck;
    }

    public void setNoAck(boolean noAck) {
        this.noAck = noAck;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 70);
    }


    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(reserved1);
        new ShortStringHelper(queue).write(rb);
        var bits = new byte[1];
        if (noAck) BBufferUtils.setBit(bits, 0);
        rb.write(bits);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var reserved1 = rb.getShort();
        var exchange = ShortStringHelper.read(rb);
        var routingKey = ShortStringHelper.read(rb);
        var bits = new byte[]{rb.get()};
        boolean noAck = BBufferUtils.getBit(bits, 0) > 0;

        var queueDeclare = new BasicGet();
        queueDeclare.setChannel(channel);
        queueDeclare.reserved1 = (reserved1);
        queueDeclare.noAck = (noAck);
        queueDeclare.queue = (exchange);

        return iteratorOfRunnable(() -> proxy.execute(context,
                connection,
                queueDeclare,
                new BasicGetOk()
        ));
    }
}
