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

public class BasicCancel extends Basic {

    protected static final JsonMapper mapper = new JsonMapper();
    private String consumerTag;
    private boolean noWait;
    private int consumeId;

    public BasicCancel() {
        super();
    }

    public BasicCancel(Class<?>... events) {
        super(events);
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public void setConsumerTag(String consumerTag) {
        this.consumerTag = consumerTag;
    }

    public boolean isNoWait() {
        return noWait;
    }

    public void setNoWait(boolean noWait) {
        this.noWait = noWait;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 30);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        new ShortStringHelper(consumerTag).write(rb);
        rb.write(noWait ? (byte) 0x01 : (byte) 0x00);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        AmqpProxy proxy = (AmqpProxy) context.getProxy();
        ProxyConnection connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);

        var toSend = new BasicCancel();
        toSend.setChannel(channel);
        toSend.consumerTag = ShortStringHelper.read(rb);
        toSend.noWait = rb.get() == 1;

        if (isProxyed()) {
            toSend.setConsumeId(basicConsume.getConsumeId());
            proxy.respond(toSend, new PluginContext("AMQP", "RESPONSE", System.currentTimeMillis(), context));

            return iteratorOfList(toSend);
        }
        var bsc = new BasicCancelOk();
        bsc.setConsumerTag(toSend.consumerTag);
        return iteratorOfRunnable(() -> {
            proxy.sendAndExpect(context, connection, basicConsume, bsc);
        });
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }
}
