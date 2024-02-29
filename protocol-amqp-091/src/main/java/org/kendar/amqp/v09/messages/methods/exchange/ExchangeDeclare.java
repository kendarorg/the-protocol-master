package org.kendar.amqp.v09.messages.methods.exchange;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Exchange;
import org.kendar.amqp.v09.utils.FieldsReader;
import org.kendar.amqp.v09.utils.FieldsWriter;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferUtils;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;
import java.util.Map;

public class ExchangeDeclare extends Exchange {
    private short reserved1;
    private String name;
    private boolean passive;
    private boolean durable;
    private boolean internal;
    private boolean autoDelete;
    private boolean noWait;
    private Map<String, Object> args;
    private String exchangeType;

    public ExchangeDeclare() {
        super();
    }

    public ExchangeDeclare(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 10);
    }

    public short getReserved1() {
        return reserved1;
    }

    public void setReserved1(short reserved1) {
        this.reserved1 = reserved1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPassive() {
        return passive;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
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
        new ShortStringHelper(name).write(rb);
        new ShortStringHelper(exchangeType).write(rb);
        var bits = new byte[1];
        if (passive) BBufferUtils.setBit(bits, 0);
        if (durable) BBufferUtils.setBit(bits, 1);
        if (autoDelete) BBufferUtils.setBit(bits, 2);
        if (internal) BBufferUtils.setBit(bits, 3);
        if (noWait) BBufferUtils.setBit(bits, 4);
        rb.write(bits);
        FieldsWriter.writeTable(args, rb);

    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var reserved1 = rb.getShort();
        var name = ShortStringHelper.read(rb);
        var exchangeType = ShortStringHelper.read(rb);
        var bits = new byte[]{rb.get()};
        boolean passive = BBufferUtils.getBit(bits, 0) > 0;
        boolean durable = BBufferUtils.getBit(bits, 1) > 0;
        boolean autoDelete = BBufferUtils.getBit(bits, 2) > 0;
        boolean internal = BBufferUtils.getBit(bits, 3) > 0;
        boolean noWait = BBufferUtils.getBit(bits, 4) > 0;
        var args = FieldsReader.readTable(rb);

        var queueDeclare = new ExchangeDeclare();
        queueDeclare.setChannel(channel);
        queueDeclare.setReserved1(reserved1);
        queueDeclare.setArgs(args);
        queueDeclare.setExchangeType(exchangeType);
        queueDeclare.setPassive(passive);
        queueDeclare.setDurable(durable);
        queueDeclare.setInternal(internal);
        queueDeclare.setAutoDelete(autoDelete);
        queueDeclare.setNoWait(noWait);
        queueDeclare.setName(name);

        var exchangeDeclare = new ExchangeDeclareOk();
        exchangeDeclare.setChannel(channel);
        return iteratorOfRunnable(() -> proxy.execute(context,
                connection,
                queueDeclare,
                exchangeDeclare
        ));
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(String exchangeType) {
        this.exchangeType = exchangeType;
    }
}
