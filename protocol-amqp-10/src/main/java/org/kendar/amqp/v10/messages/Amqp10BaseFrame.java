package org.kendar.amqp.v10.messages;

import org.kendar.amqp.v10.dtos.FrameType;
import org.kendar.amqp.v10.fsm.Amqp10FrameTranslator;
import org.kendar.amqp.v10.fsm.events.Amqp10Frame;
import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.PluginContext;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

/**
 * Base class for AMQP 1.0 performative / SASL states (analog of the v09
 * {@code Frame}). A frame is matched by its described-type ulong descriptor
 * (see {@link org.kendar.amqp.v10.dtos.Performatives}) and frame type
 * (AMQP=0 / SASL=1).
 * <p>
 * M1 does <b>byte-exact passthrough</b>: the whole raw frame is forwarded to the
 * broker ({@code sendAndForget}) for client-originated frames, or echoed to the
 * client ({@code respond} + write) for broker-originated frames ({@code asProxy}).
 * Semantic field encode/decode arrives with the M2 codec.
 */
public abstract class Amqp10BaseFrame extends ProtoState implements NetworkReturnMessage {
    private short channel;
    private byte frameType = FrameType.AMQP.asByte();
    private boolean proxyed;
    private byte[] raw;

    protected Amqp10BaseFrame() {
        super();
    }

    protected Amqp10BaseFrame(Class<?>... events) {
        super(events);
    }

    /**
     * The described-type descriptor code this state matches, or -1 for frames
     * without a descriptor (empty/heartbeat).
     */
    protected abstract long getDescriptorCode();

    /** AMQP (0) by default; SASL frames override to {@link FrameType#SASL}. */
    protected byte expectedFrameType() {
        return FrameType.AMQP.asByte();
    }

    public boolean isProxyed() {
        return proxyed;
    }

    public Amqp10BaseFrame asProxy() {
        this.proxyed = true;
        return this;
    }

    public short getChannel() {
        return channel;
    }

    public void setChannel(short channel) {
        this.channel = channel;
    }

    public byte getFrameType() {
        return frameType;
    }

    public void setFrameType(byte frameType) {
        this.frameType = frameType;
    }

    public byte[] getRaw() {
        return raw;
    }

    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    @Override
    public void write(BBuffer rb) {
        if (raw != null) {
            rb.write(raw);
            return;
        }
        throw new TPMProtocolException("Amqp10BaseFrame: no raw bytes to write (semantic encoding lands in M2)");
    }

    public boolean canRun(Amqp10Frame event) {
        if (event.getFrameType() != expectedFrameType()) {
            return false;
        }
        var rb = event.getBuffer();
        var pos = rb.getPosition();
        try {
            rb.setPosition(0);
            if (rb.size() < 8) {
                return false;
            }
            var size = rb.getInt();
            rb.get();      // doff
            var type = rb.get();
            rb.getShort(); // channel
            if (type != expectedFrameType()) {
                return false;
            }
            var body = rb.getBytes(Math.max(0, size - 8));
            return Amqp10FrameTranslator.peekDescriptorCode(body) == getDescriptorCode();
        } finally {
            rb.setPosition(pos);
        }
    }

    public Iterator<ProtoStep> execute(Amqp10Frame event) {
        var context = (NetworkProtoContext) event.getContext();
        var proxy = (NetworkProxy) context.getProxy();
        var connection = ((ProxyConnection) context.getValue("CONNECTION"));

        var rb = event.getBuffer();
        rb.setPosition(0);
        var all = rb.getBytes(rb.size());
        var message = new RawFrame(getDescriptorCode(), expectedFrameType());
        message.setChannel(event.getChannel());
        message.setRaw(all);

        if (isProxyed()) {
            proxy.respond(message, new PluginContext("AMQP10", "RESPONSE", System.currentTimeMillis(), context));
            return iteratorOfList(message);
        }
        return iteratorOfRunnable(() -> {
            proxy.sendAndForget(context, connection, message);
            drainReplayResponses(context);
        });
    }

    /**
     * In broker-less replay the replay plugin intercepts {@code sendAndForget} and
     * queues the recorded responses via {@code addResponse}; those are normally
     * drained only in {@code postWrite} (after a client write). A relay-based state
     * writes nothing here, so drain and run them now. No-op in passthrough/record
     * (nothing is queued).
     */
    public static void drainReplayResponses(NetworkProtoContext context) {
        for (var runnable : context.getRunnables()) {
            runnable.run();
        }
    }
}
