package org.kendar.amqp.v09.messages.methods.exchange;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Exchange;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferUtils;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class ExchangeDelete extends Exchange {
    private short reserved1;
    private String name;
    private boolean ifUnused;
    private boolean noWait;

    public ExchangeDelete() {
        super();
    }

    public ExchangeDelete(Class<?>... events) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIfUnused() {
        return ifUnused;
    }

    public void setIfUnused(boolean ifUnused) {
        this.ifUnused = ifUnused;
    }


    public boolean isNoWait() {
        return noWait;
    }

    public void setNoWait(boolean noWait) {
        this.noWait = noWait;
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(reserved1);
        new ShortStringHelper(name).write(rb);
        var bits = new byte[1];
        if (ifUnused) BBufferUtils.setBit(bits, 0);
        if (noWait) BBufferUtils.setBit(bits, 1);
        rb.write(bits);

    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var reserved1 = rb.getShort();
        var name = ShortStringHelper.read(rb);
        var bits = new byte[]{rb.get()};
        boolean ifUnused = BBufferUtils.getBit(bits, 0) > 0;
        boolean noWait = BBufferUtils.getBit(bits, 1) > 0;


        var queueDeclare = new ExchangeDelete();
        queueDeclare.setChannel(channel);
        queueDeclare.setReserved1(reserved1);
        queueDeclare.setIfUnused(ifUnused);
        queueDeclare.setNoWait(noWait);
        queueDeclare.setName(name);

        var deleteOk = new ExchangeDeleteOk();
        deleteOk.setChannel(channel);

        return iteratorOfRunnable(() -> proxy.execute(context,
                connection,
                queueDeclare,
                deleteOk
        ));
    }
}
