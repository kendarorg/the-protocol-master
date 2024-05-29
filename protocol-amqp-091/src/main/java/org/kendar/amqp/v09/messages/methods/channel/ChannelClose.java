package org.kendar.amqp.v09.messages.methods.channel;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Channel;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class ChannelClose extends Channel {
    private short replyCode;
    private String replyText;
    private short failingClassId;
    private short failingMethodId;

    public ChannelClose() {
        super();
    }

    public ChannelClose(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 40);
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
        var proxy = (AmqpProxy) context.getProxy();
        var connection = ((ProxyConnection) event.getContext().getValue("CONNECTION"));

        var replyCode = rb.getShort();
        var replyText = ShortStringHelper.read(rb);
        var classIdMsg = rb.getShort();
        var methodIdMsg = rb.getShort();
        var chClose = new ChannelClose();
        chClose.setChannel(channel);
        chClose.setReplyCode(replyCode);
        chClose.setFailingClassId(classIdMsg);
        chClose.setReplyText(replyText);
        chClose.setFailingMethodId(methodIdMsg);

        var result = new ChannelCloseOk();
        result.setChannel(channel);

        return iteratorOfRunnable(() -> proxy.sendAndExpect(context,
                connection,
                chClose,
                result,
                true)
        );
    }
}
