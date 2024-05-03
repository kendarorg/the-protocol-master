package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.amqp.v09.utils.ProxyedBehaviour;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

public class BasicReturn extends Basic {

    protected static final JsonMapper mapper = new JsonMapper();
    private short replyCode;
    private String replyText;
    private String exchange;
    private String routingKey;
    private int consumeId;

    public BasicReturn() {
        super();
    }

    public BasicReturn(Class<?>... events) {
        super(events);
    }

    public short getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(short replyCode) {
        this.replyCode = replyCode;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 50);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(replyCode);
        new ShortStringHelper(replyText).write(rb);
        new ShortStringHelper(exchange).write(rb);
        new ShortStringHelper(routingKey).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        AmqpProxy proxy = (AmqpProxy) context.getProxy();
        ProxyConnection connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));


        var toSend = new BasicReturn();
        toSend.setChannel(channel);
        toSend.setReplyCode(rb.getShort());
        toSend.setReplyText(ShortStringHelper.read(rb));
        toSend.setExchange(ShortStringHelper.read(rb));
        toSend.setRoutingKey(ShortStringHelper.read(rb));

        return ProxyedBehaviour.doStuff(this,context,channel,toSend,proxy,connection);
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }
}
