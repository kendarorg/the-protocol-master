package org.kendar.amqp.v10.codec;

import org.kendar.buffers.BBuffer;

import java.util.Collections;
import java.util.List;

/**
 * Small helpers to read the described performative that opens an AMQP 1.0 frame
 * body, shared by the states that need semantic fields (attach/transfer
 * correlation for the publish plugin). Header/empty frames yield {@code null}.
 */
public final class Amqp10Frames {
    private Amqp10Frames() {
    }

    /** Offset of the frame body: DOFF (byte 4) counts 4-byte words. */
    public static int bodyStart(byte[] frame) {
        return (frame[4] & 0xFF) * 4;
    }

    /** The leading described performative of the frame, or {@code null}. */
    public static DescribedType performative(byte[] frame) {
        if (frame == null || frame.length < 8) {
            return null;
        }
        if (frame[0] == 'A' && frame[1] == 'M' && frame[2] == 'Q' && frame[3] == 'P') {
            return null; // protocol header, not a frame
        }
        var bs = bodyStart(frame);
        if (bs < 8 || bs >= frame.length) {
            return null;
        }
        try {
            var bb = BBuffer.of(frame);
            bb.setPosition(bs);
            var obj = new Amqp10TypeReader().readAny(bb);
            return (obj instanceof DescribedType) ? (DescribedType) obj : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** The performative field list, or an empty list if not a described list. */
    public static List<?> fields(DescribedType dt) {
        if (dt != null && dt.getValue() instanceof List) {
            return (List<?>) dt.getValue();
        }
        return Collections.emptyList();
    }

    /** Positional field accessor, {@code null} when absent (trailing-null truncation). */
    public static Object field(List<?> fields, int index) {
        return (index >= 0 && index < fields.size()) ? fields.get(index) : null;
    }

    /**
     * Wraps a frame body (one or more described types) in the AMQP 1.0 envelope:
     * {@code size(4, includes itself)} {@code DOFF=2} {@code type} {@code channel}.
     * There is no trailing end marker (unlike 0.9.1).
     */
    public static byte[] frame(short channel, byte type, byte[] body) {
        var rb = new BBuffer();
        rb.writeInt(body.length + 8);
        rb.write((byte) 2);   // DOFF: 2 words = 8-byte header, no extended header
        rb.write(type);
        rb.writeShort(channel);
        rb.write(body);
        return rb.getAll();
    }

    /** Coerces an AMQP numeric wrapper / {@link Number} to a long, or -1. */
    public static long asLong(Object v) {
        if (v instanceof UnsignedInt) {
            return ((UnsignedInt) v).getValue();
        }
        if (v instanceof UnsignedLong) {
            return ((UnsignedLong) v).getRawBits();
        }
        if (v instanceof UnsignedShort) {
            return ((UnsignedShort) v).getValue();
        }
        if (v instanceof UnsignedByte) {
            return ((UnsignedByte) v).getValue();
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return -1;
    }
}
