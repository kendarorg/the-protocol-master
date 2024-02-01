package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.messages.frames.MethodFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;

import java.util.Iterator;

public class ConnectionTuneOk extends MethodFrame {
    public ConnectionTuneOk(){super();}
    public ConnectionTuneOk(Class<?> ...events){super(events);}

    @Override
    protected void setClassAndMethod() {
        setClassId((short) 10);
        setMethodId((short) 31);
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
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(channelMax);
        rb.writeInt(frameMax);
        rb.writeShort(hearthBeat);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, BytesEvent event) {
        var channelMax = rb.getShort();
        var frameMax = rb.getInt();
        var hearthBeat = rb.getShort();
        return iteratorOfEmpty();
    }


}
