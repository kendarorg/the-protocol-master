package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Connection;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class ConnectionCloseOk extends Connection {


    public ConnectionCloseOk() {
        super();
    }

    public ConnectionCloseOk(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 51);
    }


    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var result = new ConnectionCloseOk();
        result.setChannel(channel);
        return iteratorOfList(result);
    }
}
