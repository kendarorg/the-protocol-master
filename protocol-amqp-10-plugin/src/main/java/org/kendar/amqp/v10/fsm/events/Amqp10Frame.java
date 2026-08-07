package org.kendar.amqp.v10.fsm.events;

import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.states.TaggedObject;

import java.util.ArrayList;

/**
 * A single AMQP 1.0 frame (size|doff|type|channel|body). Carries the channel
 * <b>and</b> the frame type (AMQP=0 / SASL=1) since routing depends on both.
 * <p>
 * Unlike AMQP 0.9.1 (where channel 0 is connection-reserved and never tagged),
 * AMQP 1.0 clients may legally open their first session on channel 0. Session
 * performatives are therefore tagged by frame kind, not by {@code channel > 0}:
 * only AMQP-type frames get a {@code SESSION:<channel>} tag; SASL/empty/header
 * traffic stays untagged. The frame translator decides whether a given
 * performative is connection-scoped (open/close) and, if so, leaves it untagged.
 */
public class Amqp10Frame extends ProtocolEvent implements TaggedObject {
    private final BBuffer buffer;
    private final short channel;
    private final byte frameType;
    private final boolean sessionScoped;

    public Amqp10Frame(ProtoContext context, Class<?> prevState, BBuffer buffer, short channel, byte frameType) {
        this(context, prevState, buffer, channel, frameType, false);
    }

    public Amqp10Frame(ProtoContext context, Class<?> prevState, BBuffer buffer, short channel,
                       byte frameType, boolean sessionScoped) {
        super(context, prevState);
        this.buffer = buffer;
        this.channel = channel;
        this.frameType = frameType;
        this.sessionScoped = sessionScoped;
        this.setTags(new ArrayList<>());
        if (sessionScoped && frameType == FrameType.AMQP.asByte() && channel >= 0) {
            this.getTag().add(new Tag("SESSION", "" + channel));
        }
    }

    public short getChannel() {
        return channel;
    }

    public byte getFrameType() {
        return frameType;
    }

    public boolean isSessionScoped() {
        return sessionScoped;
    }

    public BBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "Amqp10Frame{channel=" + channel + ", frameType=" + frameType + '}';
    }
}
