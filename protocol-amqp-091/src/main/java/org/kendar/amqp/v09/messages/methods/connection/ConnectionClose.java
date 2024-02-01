package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.proxy.ProxyConnection;
import org.kendar.server.SocketChannel;

import java.util.Iterator;

public class ConnectionClose extends MethodFrame {
    private short replyCode;
    private String replyText;
    private short failingClassId;
    private short failingMethodId;

    public ConnectionClose(){super();}
    public ConnectionClose(Class<?> ...events){super(events);}
    @Override
    protected void setClassAndMethod() {
        setClassId((short) 10);
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
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        var context = (AmqpProtoContext)event.getContext();
        var connection = ((ProxyConnection)event.getContext().getValue("CONNECTION"));
        var sock = (SocketChannel)connection.getConnection();

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

        sock.write(chClose,context.buildBuffer());

        var chCloseCok = new ConnectionCloseOk();
        sock.read(chCloseCok);
        sock.close();

        return iteratorOfList(chCloseCok);
    }
}
