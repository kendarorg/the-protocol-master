package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Connection;
import org.kendar.amqp.v09.utils.ProxyedBehaviour;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.utils.JsonMapper;

import java.util.Iterator;

public class ConnectionBlocked extends Connection {
    protected static final JsonMapper mapper = new JsonMapper();
    private String reason;
    private int consumeId;

    public ConnectionBlocked() {
        super();
    }

    public ConnectionBlocked(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 60);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        new ShortStringHelper(reason).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        ProxyConnection connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));


        var replyText = ShortStringHelper.read(rb);
        var toSend = new ConnectionBlocked();
        toSend.setChannel(channel);
        toSend.setReason(replyText);


        return ProxyedBehaviour.doStuff(this, context, channel, toSend, proxy, connection);
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }
}
