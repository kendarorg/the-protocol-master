package org.kendar.amqp.v09.messages.frames;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.states.ProtoState;

/**
 * The frame-end octet MUST always be the hexadecimal value %xCE.
 */
public class GenericFrame extends ProtoState implements NetworkReturnMessage {
    private short channel = 0;
    private byte type = 0;

    public GenericFrame() {
        super();
    }

    public GenericFrame(Class<?>... events) {
        super(events);
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
        throw new RuntimeException();

    }


    public boolean canRun(BytesEvent event) {
        var rb = event.getBuffer();
        if (rb.size() < 7) {
            return false;
        }
        var pos = rb.getPosition();
        var type = rb.get();
        var channel = rb.getShort();
        var size = rb.getInt();
        var result = rb.size() >= (size + 7);
        rb.setPosition(pos);
        return result;
    }

    public BytesEvent execute(BytesEvent event) {
        var rb = event.getBuffer();
        var type = rb.get();
        var channel = rb.getShort();
        var size = rb.getInt();
        var content = rb.getBytes(size);
        var end = rb.get();
        var bb = new BBuffer();
        bb.write(type);
        bb.writeShort(channel);
        bb.writeInt(size);
        bb.write(content);
        bb.write(end);
        bb.setPosition(0);
        return new BytesEvent(null, null, bb);
    }

}
