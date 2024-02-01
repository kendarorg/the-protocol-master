package org.kendar.amqp.v09.messages.methods.channel;

import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;

public class ChannelCloseOk extends MethodFrame {


    public ChannelCloseOk(){super();}
    public ChannelCloseOk(Class<?> ...events){super(events);}
    @Override
    protected void setClassAndMethod() {
        setClassId((short) 20);
        setMethodId((short) 41);
    }




    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
//        var context = (AmqpProtoContext)event.getContext();
//        var connection = ((ProxyConnection)event.getContext().getValue("CONNECTION"));
//        var sock = (SocketChannel)connection.getConnection();


        this.setChannel(channel);
        return iteratorOfList(this);
    }
}
