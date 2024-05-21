package org.kendar.amqp.v09.messages.methods.basic;

import org.kendar.amqp.v09.AmqpProtocol;
import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Basic;
import org.kendar.amqp.v09.utils.*;
import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferUtils;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class BasicConsume extends Basic {

    private static final Logger log = LoggerFactory.getLogger(BasicConsume.class);
    private short reserved1;
    private String queue;
    private String consumerTag;
    private boolean noLocal;
    private boolean noAck;
    private boolean exclusive;
    private boolean noWait;
    private Map<String, Object> arguments;
    private int consumeId;

    public BasicConsume() {
        super();
    }

    public BasicConsume(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 20);
    }

    public short getReserved1() {
        return reserved1;
    }

    public void setReserved1(short reserved1) {
        this.reserved1 = reserved1;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public void setConsumerTag(String consumerTag) {
        this.consumerTag = consumerTag;
    }

    public boolean isNoLocal() {
        return noLocal;
    }

    public void setNoLocal(boolean noLocal) {
        this.noLocal = noLocal;
    }

    public boolean isNoAck() {
        return noAck;
    }

    public void setNoAck(boolean noAck) {
        this.noAck = noAck;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public boolean isNoWait() {
        return noWait;
    }

    public void setNoWait(boolean noWait) {
        this.noWait = noWait;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    @Override
    protected void writePreArguments(BBuffer rb) {

        rb.writeShort(reserved1);
        new ShortStringHelper(queue).write(rb);
        new ShortStringHelper(consumerTag).write(rb);
        var bits = new byte[1];
        if (noLocal) BBufferUtils.setBit(bits, 0);
        if (noAck) BBufferUtils.setBit(bits, 1);
        if (exclusive) BBufferUtils.setBit(bits, 2);
        if (noWait) BBufferUtils.setBit(bits, 3);
        rb.write(bits);
        FieldsWriter.writeTable(arguments, rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));
        var sock = (AmqpProxySocket) connection.getConnection();

        this.setChannel(channel);
        var reserved1 = rb.getShort();
        var queue = ShortStringHelper.read(rb);
        var consumerTag = ShortStringHelper.read(rb);
        var bits = new byte[]{rb.get()};
        boolean noLocal = BBufferUtils.getBit(bits, 0) > 0;
        boolean noAck = BBufferUtils.getBit(bits, 1) > 0;
        boolean exclusive = BBufferUtils.getBit(bits, 2) > 0;
        boolean noWait = BBufferUtils.getBit(bits, 3) > 0;
        var arguments = FieldsReader.readTable(rb);

        var basicConsume = new BasicConsume();
        basicConsume.setChannel(channel);
        basicConsume.arguments = arguments;
        basicConsume.reserved1 = reserved1;
        basicConsume.noWait = noWait;
        basicConsume.exclusive = exclusive;
        basicConsume.noAck = noAck;
        basicConsume.consumerTag = consumerTag;
        basicConsume.noLocal = noLocal;
        basicConsume.queue = queue;
        basicConsume.setConsumeId(ProtoDescriptor.getCounter("CONSUME_ID"));

        AmqpProtocol.consumeContext.put(basicConsume.getConsumeId(), context);

        context.setValue("BASIC_CONSUME_CH_" + channel, basicConsume);
        log.debug("CTX:" + context.getContextId() + " CHAN:" + channel + " CNS_ID:" + basicConsume.getConsumeId());

        context.setValue("BASIC_CONSUME_CI_" + basicConsume.getConsumeId(), basicConsume);

        //Send back the consume ok
        return iteratorOfRunnable(() -> proxy.execute(context,
                connection,
                basicConsume,
                new BasicConsumeOk()
        ));
    }

    public int getConsumeId() {
        return consumeId;
    }

    public void setConsumeId(int consumeId) {
        this.consumeId = consumeId;
    }
}
