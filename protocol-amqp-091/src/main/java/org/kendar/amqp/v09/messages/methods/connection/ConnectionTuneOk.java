package org.kendar.amqp.v09.messages.methods.connection;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.amqp.v09.messages.methods.Connection;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ProtoStep;

import java.util.Iterator;

public class ConnectionTuneOk extends Connection {
    private short channelMax;
    private int frameMax;
    private short hearthBeat;

    public ConnectionTuneOk() {
        super();
    }

    public ConnectionTuneOk(Class<?>... events) {
        super(events);
    }

    @Override
    protected void setMethod() {
        setMethodId((short) 31);
    }

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

    @Override
    protected void writePreArguments(BBuffer rb) {
        rb.writeShort(channelMax);
        rb.writeInt(frameMax);
        rb.writeShort(hearthBeat);
    }

    @Override
    protected Iterator<ProtoStep> executeMethod(short channel, short classId, short methodId, BBuffer rb, AmqpFrame event) {
        var channelMax = rb.getShort();
        var frameMax = rb.getInt();
        var hearthBeat = rb.getShort();
        return iteratorOfEmpty();
    }


}
