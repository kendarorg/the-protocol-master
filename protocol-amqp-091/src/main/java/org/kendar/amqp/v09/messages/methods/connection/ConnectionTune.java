package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConnectionTune extends MethodFrame {
    public ConnectionTune(){super();}
    public ConnectionTune(Class<?> ...events){super(events);}

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 10);
        setMethodId((short) 30);
    }
    private short channelMax;
    private int frameMax;

    public short getChannelMax() {
        return channelMax;
    }

    public void setChannelMax(short channelMax) {
        this.channelMax = channelMax;
    }

    public int getFrameMax() {
        return frameMax;
    }

    public void setFrameMax(int frameMax) {
        this.frameMax = frameMax;
    }

    public short getHearthBeat() {
        return hearthBeat;
    }

    public void setHearthBeat(short hearthBeat) {
        this.hearthBeat = hearthBeat;
    }

    private short hearthBeat;
    @Override
    protected Map<String, Object> retrieveMethodArguments() {
        return new HashMap<>();
    }

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(channelMax);
        rb.writeInt(frameMax);
        rb.writeShort(hearthBeat);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        this.channelMax = rb.getShort();
        this.frameMax =rb.getInt();
        this.hearthBeat = rb.getShort();
        return iteratorOfList(this);
    }


}
