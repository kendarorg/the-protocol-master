package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.messages.methods.Connection;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class ConnectionOpen extends Connection {
    private String vhost;
    private String reserved1;
    private byte reserved2;


    public ConnectionOpen() {
        super();
    }

    public ConnectionOpen(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 40);
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getReserved1() {
        return reserved1;
    }

    public void setReserved1(String reserved1) {
        this.reserved1 = reserved1;
    }

    public byte getReserved2() {
        return reserved2;
    }

    public void setReserved2(byte reserved2) {
        this.reserved2 = reserved2;
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        new ShortStringHelper(vhost).write(rb);
        new ShortStringHelper(reserved1).write(rb);
        rb.write(reserved2);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        var context = (AmqpProtoContext) event.getContext();
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var vhost = ShortStringHelper.read(rb);
        var reserved1 = ShortStringHelper.read(rb);
        var reserved2 = rb.get();
        context.setValue("VHOST", vhost);
        context.setValue("RESERVED1", reserved1);

        var conOpen = new ConnectionOpen();
        conOpen.setVhost(vhost);
        conOpen.setReserved1(reserved1);
        conOpen.setReserved2(reserved2);

        return iteratorOfRunnable(() -> {
                    var message = proxy.execute(context,
                            connection,
                            conOpen,
                            new ConnectionOpenOk()
                    );

                    message.setReserved1(reserved1);
                    return message;
                }
        );
    }


}
