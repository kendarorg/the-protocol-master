package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.context.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferUtils;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;


public class BasicPublish extends Basic {
    private String exchange;
    private short reserved1;
    private String routingKey;
    private boolean mandatory;
    private boolean immediate;

    public BasicPublish() {
        super();
    }

    public BasicPublish(Class<?>... events) {
        super(events);
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public short getReserved1() {
        return reserved1;
    }

    public void setReserved1(short reserved1) {
        this.reserved1 = reserved1;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 40);
    }


    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(reserved1);
        new ShortStringHelper(exchange).write(rb);
        new ShortStringHelper(routingKey).write(rb);
        var bits = new byte[1];
        if (mandatory) BBufferUtils.setBit(bits, 0);
        if (immediate) BBufferUtils.setBit(bits, 1);
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
        boolean mandatory = BBufferUtils.getBit(bits, 0) > 0;
        boolean immediate = BBufferUtils.getBit(bits, 1) > 0;

        var queueDeclare = new BasicPublish();
        queueDeclare.setChannel(channel);
        queueDeclare.reserved1 = (reserved1);
        queueDeclare.mandatory = (mandatory);
        queueDeclare.immediate = (immediate);
        queueDeclare.routingKey = (routingKey);
        queueDeclare.exchange = (exchange);

        context.setValue("BASIC_PUBLISH_RK_" + channel, routingKey);
        context.setValue("BASIC_PUBLISH_XC_" + channel, exchange);

        return iteratorOfRunnable(() -> proxy.sendAndForget(context,
                connection,
                queueDeclare
        ));
    }
}
