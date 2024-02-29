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

public class BasicCancelOk extends Basic {

    private String consumerTag;

    public BasicCancelOk() {
        super();
    }

    public BasicCancelOk(Class<?>... events) {
        super(events);
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public void setConsumerTag(String consumerTag) {
        this.consumerTag = consumerTag;
    }


    @Override
    protected void setMethod() {
        setMethodId((short) 31);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        new ShortStringHelper(consumerTag).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        AmqpProxy proxy = (AmqpProxy) context.getProxy();
        ProxyConnection connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));


        var bd = new BasicCancelOk();
        bd.setChannel(channel);
        bd.consumerTag = ShortStringHelper.read(rb);


        return iteratorOfList(this);
    }
}
