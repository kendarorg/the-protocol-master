package org.kendar.amqp.v09.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.states.TaggedObject;

import java.util.ArrayList;

public class AmqpFrame extends ProtocolEvent implements TaggedObject {
    private final BBuffer buffer;
    private final short channel;

    public AmqpFrame(ProtoContext context, Class<?> prevState, BBuffer buffer, short channel) {
        super(context, prevState);
        this.buffer = buffer;
        this.channel = channel;
        this.setTags(new ArrayList<>());
        if (channel > 0) {
            this.getTag().add(new Tag("CHANNEL", "" + channel));
        }
    }

    public short getChannel() {
        return channel;
    }

    public BBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "AmqpFrame{" +
                "channel=" + channel +
                '}';
    }
}
