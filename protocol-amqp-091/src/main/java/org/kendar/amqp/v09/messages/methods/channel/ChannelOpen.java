package org.kendar.amqp.v09.messages.methods.channel;

import org.kendar.amqp.v09.AmqpProxy;
import org.kendar.amqp.v09.executor.AmqpProtoContext;
import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

public class ChannelOpen extends MethodFrame {
    public ChannelOpen(){super();}
    public ChannelOpen(Class<?> ...events){super(events);}
    @Override
    protected void setClassAndMethod() {
        setClassId((short) 20);
        setMethodId((short) 10);
    }

    public String getReserved1() {
        return reserved1;
    }

    public void setReserved1(String reserved1) {
        this.reserved1 = reserved1;
    }

    private String reserved1;

    @Override
    protected void writePreArguments(BBuffer rb) {
        new ShortStringHelper(reserved1).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        var context = (AmqpProtoContext)event.getContext();
        var proxy = (AmqpProxy)context.getProxy();
        var connection = ((ProxyConnection)event.getContext().getValue("CONNECTION"));

        var reserved1 = ShortStringHelper.read(rb);

        var channelOpen = new ChannelOpen();
        channelOpen.setChannel(channel);
        channelOpen.setReserved1(reserved1);
        var channelOpenOk = proxy.execute(
                connection,
                channelOpen,
                new ChannelOpenOk()
        );

        return iteratorOfList(channelOpenOk);
    }
}
