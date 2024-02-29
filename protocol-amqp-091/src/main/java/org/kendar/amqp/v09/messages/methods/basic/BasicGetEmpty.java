package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

public class BasicGetEmpty extends Basic {

    protected static JsonMapper mapper = new JsonMapper();
    private String reserved1;
    private int consumeId;

    public BasicGetEmpty() {
        super();
    }

    public BasicGetEmpty(Class<?>... events) {
        super(events);
    }

    public String getReserved1() {
        return reserved1;
    }

    public void setReserved1(String reserved1) {
        this.reserved1 = reserved1;
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 30);
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        new ShortStringHelper(reserved1).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        AmqpProxy proxy = (AmqpProxy) context.getProxy();
        ProxyConnection connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        //var basicConsume = (BasicConsume) context.getValue("BASIC_CONSUME_CH_" + channel);

        var toSend = new BasicGetEmpty();
        toSend.setChannel(channel);
        toSend.reserved1 = ShortStringHelper.read(rb);
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
