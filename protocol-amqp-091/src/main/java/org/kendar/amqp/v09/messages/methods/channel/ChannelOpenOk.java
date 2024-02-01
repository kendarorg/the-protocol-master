package org.kendar.amqp.v09.messages.methods.channel;

import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.amqp.v09.utils.LongStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;

public class ChannelOpenOk extends MethodFrame {
    public ChannelOpenOk(){super();}
    public ChannelOpenOk(Class<?> ...events){super(events);}
    @Override
    protected void setClassAndMethod() {
        setClassId((short) 20);
        setMethodId((short) 11);
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
        new LongStringHelper(reserved1).write(rb);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {

        this.setChannel(channel);
        this.reserved1 = LongStringHelper.read(rb);


        return iteratorOfList(this);
    }
}
