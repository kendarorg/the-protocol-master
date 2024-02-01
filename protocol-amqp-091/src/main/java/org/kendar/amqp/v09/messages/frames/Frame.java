package org.kendar.amqp.v09.messages.frames;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.ReturnMessage;
import org.kendar.protocol.fsm.ProtoState;

import java.util.Iterator;

/**
 * The frame-end octet MUST always be the hexadecimal value %xCE.
 */
public abstract class Frame extends ProtoState implements ReturnMessage {
    public Frame(){
        super();
    }
    public Frame(Class<?>...events){
        super(events);
    }
    private short channel=0;
    private byte type =0;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }


    public short getChannel() {
        return channel;
    }

    public void setChannel(short channel) {
        this.channel = channel;
    }

    @Override
    public void write(BBuffer rb) {
        rb.write(getType());
        rb.writeShort(getChannel());
        var sizePos = rb.getPosition();
        rb.writeInt(0);
        var sizeStartPos = rb.getPosition();
        writeFrameContent(rb);
        var sizeEndPos = rb.getPosition();
        rb.writeInt(sizeEndPos-sizeStartPos,sizePos);
        rb.write((byte)0xCE);

    }

    protected abstract void writeFrameContent(BBuffer rb);

    public boolean canRun(BytesEvent event) {
        var rb = event.getBuffer();
        if(rb.size()<7){
            return false;
        }
        var pos =rb.getPosition();
        var type = rb.get();
        var channel = rb.getShort();
        var size = rb.getInt();
        var result= type == getType()
                && rb.size() >= (size+7)
                && canRunFrame(event);
        rb.setPosition(pos);
        return result;
    }

    protected abstract boolean canRunFrame(BytesEvent event);
    public Iterator<ProtoStep> execute(BytesEvent event) {
        var rb = event.getBuffer();
        var type = rb.get();
        var channel = rb.getShort();
        var size = rb.getInt();
        var result = executeFrame(channel,rb,event);
        rb.get();
        return result;
    }

    protected abstract Iterator<ProtoStep> executeFrame(short channel, BBuffer rb, BytesEvent event);
}
