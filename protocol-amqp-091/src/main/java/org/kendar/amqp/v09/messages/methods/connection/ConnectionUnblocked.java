package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Connection;
import org.kendar.amqp.v09.messages.methods.basic.BasicConsume;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

public class ConnectionUnblocked extends Connection {


    protected static JsonMapper mapper = new JsonMapper();
    private int consumeId;

    public ConnectionUnblocked() {
        super();
    }

    public ConnectionUnblocked(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 61);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        AmqpProxy proxy = (AmqpProxy) context.getProxy();
        ProxyConnection connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var toSend = new ConnectionUnblocked();
        toSend.setChannel(channel);


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
