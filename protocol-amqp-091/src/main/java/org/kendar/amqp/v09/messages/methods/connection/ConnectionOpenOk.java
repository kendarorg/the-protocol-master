package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.amqp.v09.utils.ShortStringHelper;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;

public class ConnectionOpenOk extends MethodFrame {
    public ConnectionOpenOk(){super();}
    public ConnectionOpenOk(Class<?> ...events){super(events);}

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 10);
        setMethodId((short) 41);
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
        this.reserved1 = ShortStringHelper.read(rb);
        return iteratorOfList(this);
    }


}
