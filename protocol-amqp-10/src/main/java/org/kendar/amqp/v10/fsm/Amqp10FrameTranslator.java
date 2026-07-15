package org.kendar.amqp.v10.fsm;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.fsm.events.Amqp10Frame;
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
 * Splits the inbound byte stream into AMQP 1.0 frames and emits {@link Amqp10Frame}.
 * <p>
 * Envelope: {@code size(4, big-endian, INCLUDES itself) doff(1) type(1) channel(2) body}.
 * There is <b>no</b> trailing {@code 0xCE} end marker (unlike 0.9.1). An 8-byte
 * protocol header ({@code AMQP ...}) is handled by {@code ProtocolHeader}, not here.
 */
public class Amqp10FrameTranslator extends ProtoState implements NetworkReturnMessage, InterruptProtoState {
    private final Logger log = LoggerFactory.getLogger(Amqp10FrameTranslator.class);

    public Amqp10FrameTranslator() {
        super();
    }

    public Amqp10FrameTranslator(Class<?>... events) {
        super(events);
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
        var head = rb.getBytes(0, 4);
        // 8-byte protocol header, not a frame -> let ProtocolHeader consume it
        if (head[0] == 'A' && head[1] == 'M' && head[2] == 'Q' && head[3] == 'P') {
            return false;
        }
        var size = rb.getInt();
        rb.setPosition(0);
        if (size < 8) {
            // malformed / not enough context yet
            return false;
        }
        if (rb.size() < size) {
            throw new AskMoreDataException();
        }
        return true;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var rb = event.getBuffer();
        var size = rb.getInt();
        var doff = rb.get();
        var type = rb.get();
        var channel = rb.getShort();
        var body = rb.getBytes(size - 8);

        // Repackage the WHOLE raw frame -> byte-exact passthrough
        var bb = new BBuffer();
        bb.writeInt(size);
        bb.write(doff);
        bb.write(type);
        bb.writeShort(channel);
        bb.write(body);
        bb.setPosition(0);

        var descriptor = peekDescriptorCode(body);
        var sessionScoped = isSessionScoped(descriptor);
        log.debug("Amqp10FrameTranslator: size={}, doff={}, type={}, channel={}, descriptor={}",
                size, doff, type, channel, descriptor);
        event.getContext().send(new Amqp10Frame(event.getContext(), event.getPrevState(), bb, channel, type, sessionScoped));
        return iteratorOfEmpty();
    }

    /**
     * Peeks the described-type ulong descriptor at the start of the frame body.
     * Returns -1 for an empty body (heartbeat) or a non-ulong descriptor.
     */
    public static long peekDescriptorCode(byte[] body) {
        if (body == null || body.length == 0) {
            return -1;
        }
        int p = 0;
        if ((body[p] & 0xFF) != 0x00) {
            return -1; // not a described type
        }
        p++;
        if (p >= body.length) {
            return -1;
        }
        int formatCode = body[p++] & 0xFF;
        switch (formatCode) {
            case 0x44: // ulong0
                return 0L;
            case 0x53: // smallulong
                return p < body.length ? (body[p] & 0xFFL) : -1;
            case 0x80: { // ulong
                long v = 0;
                for (int i = 0; i < 8 && p < body.length; i++) {
                    v = (v << 8) | (body[p++] & 0xFFL);
                }
                return v;
            }
            default:
                return -1;
        }
    }

    private static boolean isSessionScoped(long descriptor) {
        return descriptor == Performatives.BEGIN
                || descriptor == Performatives.ATTACH
                || descriptor == Performatives.FLOW
                || descriptor == Performatives.TRANSFER
                || descriptor == Performatives.DISPOSITION
                || descriptor == Performatives.DETACH
                || descriptor == Performatives.END;
    }
}
