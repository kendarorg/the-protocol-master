package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Connection;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class ConnectionClose extends Connection {
    private short replyCode;
    private String replyText;
    private short failingClassId;
    private short failingMethodId;

    public ConnectionClose() {
        super();
    }

    public ConnectionClose(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 50);
    }


    public short getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(short replyCode) {
        this.replyCode = replyCode;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public short getFailingClassId() {
        return failingClassId;
    }

    public void setFailingClassId(short failingClassId) {
        this.failingClassId = failingClassId;
    }

    public short getFailingMethodId() {
        return failingMethodId;
    }

    public void setFailingMethodId(short failingMethodId) {
        this.failingMethodId = failingMethodId;
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(this.replyCode);
        new ShortStringHelper(replyText).write(rb);
        rb.writeShort(this.failingClassId);
        rb.writeShort(this.failingMethodId);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var context = (AmqpProtoContext) event.getContext();
        AmqpProxy proxy = null;
        ProxyConnection connection = null;
        if (context != null) {
            proxy = (AmqpProxy) context.getProxy();
            connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));
        }

        var replyCode = rb.getShort();
        var replyText = ShortStringHelper.read(rb);
        var classIdMsg = rb.getShort();
        var methodIdMsg = rb.getShort();
        var chClose = new ConnectionClose();
        chClose.setChannel(channel);
        chClose.setReplyCode(replyCode);
        chClose.setFailingClassId(classIdMsg);
        chClose.setReplyText(replyText);
        chClose.setFailingMethodId(methodIdMsg);


        if (context != null) {
            var result = new ConnectionCloseOk();
            result.setChannel(channel);
            var fproxy = proxy;
            var fconn = connection;
            return iteratorOfRunnable(() -> fproxy.execute(context,
                            fconn,
                            chClose,
                            result,
                            true
                    )
            );
        }
        return iteratorOfEmpty();
    }
}
