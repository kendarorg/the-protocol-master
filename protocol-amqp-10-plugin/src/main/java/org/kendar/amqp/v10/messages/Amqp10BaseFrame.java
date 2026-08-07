package org.kendar.amqp.v10.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kendar.amqp.v10.codec.Amqp10FrameDescriber;
import org.kendar.amqp.v10.codec.Amqp10FrameEncoder;
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
import java.util.Map;

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
    private static final org.kendar.utils.JsonMapper jsonMapper = new org.kendar.utils.JsonMapper();
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

    @JsonIgnore
    public byte[] getRaw() {
        return raw;
    }

    /**
     * Human-readable view of {@link #getRaw()} for recordings (performative name,
     * named fields, message sections). This is the primary stored representation:
     * replay re-encodes the frame from it via {@code Amqp10FrameEncoder} unless a
     * {@code raw} fallback is present (see {@link #getRawBase64()}).
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getDecoded() {
        return Amqp10FrameDescriber.describe(raw);
    }

    /**
     * Raw-bytes fallback, serialized ONLY when {@code decoded} does not survive an
     * encode→describe round-trip through the JSON representation (unknown
     * descriptor, lossy value, codec gap). When this returns null the readable
     * {@code decoded} tree alone fully determines the replayed frame.
     */
    @JsonProperty("raw")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getRawBase64() {
        if (raw == null) {
            return null;
        }
        try {
            var decoded = getDecoded();
            if (decoded != null) {
                var canonical = canonical(decoded);
                var redecoded = Amqp10FrameDescriber.describe(Amqp10FrameEncoder.encode(canonical));
                if (canonical.equals(canonical(redecoded))) {
                    return null;
                }
            }
        } catch (Exception e) {
            // fall through: keep the raw fallback
        }
        return java.util.Base64.getEncoder().encodeToString(raw);
    }

    /** The tree as replay will see it after a JSON round-trip (number types normalized). */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> canonical(Map<String, Object> tree) {
        return jsonMapper.deserialize(jsonMapper.serialize(tree), Map.class);
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

        capture(context, all, isProxyed());

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
     * Hook for semantic side-tracking of a frame as it is relayed (attach/transfer
     * correlation for the publish plugin). {@code proxyed} is {@code true} for
     * broker-originated frames. Default is a no-op; passthrough is unaffected.
     */
    protected void capture(NetworkProtoContext context, byte[] raw, boolean proxyed) {
    }

    /** Reads the big-endian frame channel (bytes 6-7) from a raw AMQP 1.0 frame. */
    protected static short channelOf(byte[] raw) {
        if (raw == null || raw.length < 8) {
            return -1;
        }
        return (short) (((raw[6] & 0xFF) << 8) | (raw[7] & 0xFF));
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
