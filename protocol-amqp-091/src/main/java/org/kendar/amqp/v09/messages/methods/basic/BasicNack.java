package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

public class BasicNack extends Basic {

    protected static JsonMapper mapper = new JsonMapper();
    private long deliveryTag;
    private boolean multiple;
    private boolean requeue;
    private int consumeId;

    public BasicNack() {
        super();
    }

    public BasicNack(Class<?>... events) {
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
        byte toWrite = (byte) ((byte) (multiple ? 0x01 : 0x00) & ((byte) (requeue ? 0x02 : 0x00)));
        rb.write(toWrite);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var toSend = new BasicNack();
        toSend.setChannel(channel);
        toSend.setDeliveryTag(rb.getLong());
        var get = rb.get();
        toSend.setMultiple((get & 0x01) == 0x01);
        toSend.setRequeue((get & 0x02) == 0x02);

        if (isProxyed()) {
            var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);
            toSend.setConsumeId(basicConsume.getConsumeId());
            var storage = proxy.getStorage();
            var res = "{\"type\":\"" + toSend.getClass().getSimpleName() + "\",\"data\":" +
                    mapper.serialize(toSend) + "}";


            storage.write(
                    null
                    , mapper.toJsonNode(res)
                    , 0, "RESPONSE", "AMQP");
            return iteratorOfList(toSend);
        }
        return iteratorOfRunnable(() -> {
            proxy.execute(context, connection, toSend);
        });


    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }
}
