package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.context.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.PluginContext;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

public class BasicDeliver extends Basic {

    protected static final JsonMapper mapper = new JsonMapper();
    private String consumerTag;
    private long deliveryTag;
    private boolean redelivered;
    private String exchange;
    private String routingKey;
    private int consumeId;
    private String consumeOrigin;

    public BasicDeliver() {
        super();
    }

    public BasicDeliver(Class<?>... events) {
        super(events);
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public void setConsumerTag(String consumerTag) {
        this.consumerTag = consumerTag;
    }

    public long getDeliveryTag() {
        return deliveryTag;
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }

    public boolean isRedelivered() {
        return redelivered;
    }

    public void setRedelivered(boolean redelivered) {
        this.redelivered = redelivered;
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
        setMethodId((short) 60);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        new ShortStringHelper(consumerTag).write(rb);
        rb.writeLong(deliveryTag);
        rb.write(redelivered ? (byte) 0x01 : (byte) 0x00);
        new ShortStringHelper(exchange).write(rb);
        new ShortStringHelper(routingKey).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        AmqpProxy proxy;
        ProxyConnection connection = null;

        proxy = (AmqpProxy) context.getProxy();
        event.getContext().getValue("CONNECTION");

        var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);

        var bd = new BasicDeliver();
        bd.setChannel(channel);
        bd.setConsumeId(basicConsume.getConsumeId());
        bd.consumerTag = ShortStringHelper.read(rb);
        bd.deliveryTag = rb.getLong();
        bd.redelivered = rb.get() == 1;
        bd.exchange = ShortStringHelper.read(rb);
        bd.routingKey = ShortStringHelper.read(rb);

        var consumeOrigin = basicConsume.getQueue() + "|" + basicConsume.getChannel() + "|" + mapper.serialize(basicConsume.getArguments());
        bd.setConsumeOrigin(consumeOrigin);


        proxy.respond(bd, new PluginContext("AMQP", "RESPONSE", System.currentTimeMillis(), context));
        return iteratorOfList(bd);
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }

    public String getConsumeOrigin() {
        return consumeOrigin;
    }

    public void setConsumeOrigin(String consumeOrigin) {
        this.consumeOrigin = consumeOrigin;
    }
}
