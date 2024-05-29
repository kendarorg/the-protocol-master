package org.kendar.amqp.v09.messages.methods.exchange;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Exchange;
import org.kendar.amqp.v09.utils.FieldsReader;
import org.kendar.amqp.v09.utils.FieldsWriter;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;
import java.util.Map;

public class ExchangeUnbind extends Exchange {
    private short reserved1;
    private String destination;
    private boolean noWait;
    private Map<String, Object> args;
    private String source;
    private String routingKey;

    public ExchangeUnbind() {
        super();
    }

    public ExchangeUnbind(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 40);
    }

    public short getReserved1() {
        return reserved1;
    }

    public void setReserved1(short reserved1) {
        this.reserved1 = reserved1;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }


    public boolean isNoWait() {
        return noWait;
    }

    public void setNoWait(boolean noWait) {
        this.noWait = noWait;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(reserved1);
        new ShortStringHelper(destination).write(rb);
        new ShortStringHelper(source).write(rb);
        new ShortStringHelper(routingKey).write(rb);
        rb.write((byte) (noWait ? 1 : 0));
        FieldsWriter.writeTable(args, rb);

    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var reserved1 = rb.getShort();
        var destination = ShortStringHelper.read(rb);
        var source = ShortStringHelper.read(rb);
        var routingKey = ShortStringHelper.read(rb);
        var noWait = rb.get() == 1;
        var args = FieldsReader.readTable(rb);

        var queueDeclare = new ExchangeUnbind();
        queueDeclare.setChannel(channel);
        queueDeclare.setReserved1(reserved1);
        queueDeclare.setArgs(args);
        queueDeclare.setSource(source);
        queueDeclare.setRoutingKey(routingKey);
        queueDeclare.setNoWait(noWait);
        queueDeclare.setDestination(destination);

        return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                connection,
                queueDeclare,
                new ExchangeUnbindOk()
        ));
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}
