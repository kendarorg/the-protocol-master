package org.kendar.amqp.v09.messages.frames;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;

import java.util.Iterator;

/**
 * The frame-end octet MUST always be the hexadecimal value %xCE.
 */
public abstract class Frame extends ProtoState implements NetworkReturnMessage {
    private short channel = 0;
    private byte type = 0;
    private boolean proxyed;

    public Frame() {
        super();
    }

    public Frame(Class<?>... events) {
        super(events);
    }

    public boolean isProxyed() {
        return proxyed;
    }

    public Frame asProxy() {
        this.proxyed = true;
        return this;
    }

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
        rb.writeInt(sizeEndPos - sizeStartPos, sizePos);
        rb.write((byte) 0xCE);

    }

    protected abstract void writeFrameContent(BBuffer rb);

    public boolean canRun(AmqpFrame event) {
        var rb = event.getBuffer();
        if (rb.size() < 7) {
            return false;
        }
        var pos = rb.getPosition();
        var type = rb.get();
        if (type < 0) return false;
        var channel = rb.getShort();
        var size = rb.getInt();
        var result = false;
        if (rb.size() >= (size + 7)) {
            result = type == getType()
                    && canRunFrame(event);
        } else {
            rb.setPosition(pos);
            throw new AskMoreDataException();
        }
        rb.setPosition(pos);
        return result;
    }

    protected abstract boolean canRunFrame(AmqpFrame event);

    public Iterator<ProtoStep> execute(AmqpFrame event) {
        var rb = event.getBuffer();
        var type = rb.get();
        var channel = rb.getShort();
        var size = rb.getInt();
        var result = executeFrame(channel, rb, event, size);
        rb.get();
        return result;
    }

    protected abstract Iterator<ProtoStep> executeFrame(short channel, BBuffer rb, AmqpFrame event, int size);
}
