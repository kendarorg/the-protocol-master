package org.kendar.amqp.v09.messages.methods.channel;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Channel;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class ChannelCloseOk extends Channel {


    public ChannelCloseOk() {
        super();
    }

    public ChannelCloseOk(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 41);
    }


    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var result = new ChannelCloseOk();
        result.setChannel(channel);
        return iteratorOfList(result);
    }
}
