package org.kendar.amqp.v09.fsm;

import org.kendar.amqp.v09.fsm.events.AmqpFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * The frame-end octet MUST always be the hexadecimal value %xCE.
 */
public class AmqpFrameTranslator extends ProtoState implements NetworkReturnMessage, InterruptProtoState {
    private final Logger log = LoggerFactory.getLogger(AmqpFrameTranslator.class);
    private short channel = 0;
    private byte type = 0;

    public AmqpFrameTranslator() {
        super();
    }

    public AmqpFrameTranslator(Class<?>... events) {
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
        throw new TPMProtocolException("Not implemented");

    }


    public boolean canRun(BytesEvent event) {
        var rb = event.getBuffer();
        rb.setPosition(0);
        if (rb.size() < 8) {
            return false;
        }
        var bytes = rb.getBytes(0, 8);
        var isHeader = bytes[0] == 'A' && bytes[1] == 'M' && bytes[2] == 'Q' && bytes[3] == 'P';
        if (isHeader) return false;
        var type = rb.get();
        var channel = rb.getShort();
        var size = rb.getInt();
        if (!(rb.size() >= (size + 7))) {
            rb.setPosition(0);
            throw new AskMoreDataException();
        }
        rb.setPosition(0);
        return true;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
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
//        if(type==8){
//            return iteratorOfEmpty();
//        }
        bb.setPosition(0);

        log.debug("AmqpFrameTranslator: type = {}, channel = {}, size = {}, content = {}", type, channel, size, content);
        event.getContext().send(new AmqpFrame(event.getContext(), event.getPrevState(), bb, channel));
        return iteratorOfEmpty();
    }

}
