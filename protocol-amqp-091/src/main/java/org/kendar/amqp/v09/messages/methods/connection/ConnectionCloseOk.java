package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;

public class ConnectionCloseOk extends MethodFrame {


    public ConnectionCloseOk(){super();}
    public ConnectionCloseOk(Class<?> ...events){super(events);}
    @Override
    protected void setClassAndMethod() {
        setClassId((short) 10);
        setMethodId((short) 51);
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
