package org.kendar.amqp.v09.fsm.events;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.states.TaggedObject;

import java.util.ArrayList;

public class AmqpFrame extends BaseEvent implements TaggedObject {
    private final BBuffer buffer;
    private final short channel;

    public AmqpFrame(ProtoContext context, Class<?> prevState, BBuffer buffer, short channel) {
        super(context, prevState);
//        if(channel==-1) {
//            System.out.println("[PROXY][RECEIVED] " + buffer.toHexStringUpToLength(0, 12));
//        }else{
//            System.out.println("[OTHER][RECEIVED] " + buffer.toHexStringUpToLength(0, 12));
//        }
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
}
