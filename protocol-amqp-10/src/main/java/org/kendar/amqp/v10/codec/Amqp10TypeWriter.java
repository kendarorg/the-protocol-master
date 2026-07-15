package org.kendar.amqp.v10.codec;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.TPMProtocolException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes AMQP 1.0 values, choosing the smallest legal encoding. Mirrors
 * {@link Amqp10TypeReader}. {@link #writeDescribed(BBuffer, long, List)} encodes
 * a performative/section (described type over a list) and truncates trailing
 * null fields as the spec allows.
 */
public class Amqp10TypeWriter {

    public void writeAny(BBuffer rb, Object v) {
        if (v == null) {
            rb.write((byte) 0x40);
        } else if (v instanceof Boolean) {
            rb.write((byte) ((Boolean) v ? 0x41 : 0x42));
        } else if (v instanceof UnsignedByte) {
            rb.write((byte) 0x50);
            rb.write((byte) ((UnsignedByte) v).getValue());
        } else if (v instanceof UnsignedShort) {
            rb.write((byte) 0x60);
            rb.writeShort((short) ((UnsignedShort) v).getValue());
        } else if (v instanceof UnsignedInt) {
            long u = ((UnsignedInt) v).getValue();
            if (u == 0) {
                rb.write((byte) 0x43);
            } else if (u <= 0xFF) {
                rb.write((byte) 0x52);
                rb.write((byte) u);
            } else {
                rb.write((byte) 0x70);
                rb.writeInt((int) u);
            }
        } else if (v instanceof UnsignedLong) {
            long raw = ((UnsignedLong) v).getRawBits();
            if (raw == 0) {
                rb.write((byte) 0x44);
            } else if (Long.compareUnsigned(raw, 0xFF) <= 0) {
                rb.write((byte) 0x53);
                rb.write((byte) raw);
            } else {
                rb.write((byte) 0x80);
                rb.writeLong(raw);
            }
        } else if (v instanceof Byte) {
            rb.write((byte) 0x51);
            rb.write((Byte) v);
        } else if (v instanceof Short) {
            rb.write((byte) 0x61);
            rb.writeShort((Short) v);
        } else if (v instanceof Integer) {
            int i = (Integer) v;
            if (i >= -128 && i <= 127) {
                rb.write((byte) 0x54);
                rb.write((byte) i);
            } else {
                rb.write((byte) 0x71);
                rb.writeInt(i);
            }
        } else if (v instanceof Long) {
            long l = (Long) v;
            if (l >= -128 && l <= 127) {
                rb.write((byte) 0x55);
                rb.write((byte) l);
            } else {
                rb.write((byte) 0x81);
                rb.writeLong(l);
            }
        } else if (v instanceof Float) {
            rb.write((byte) 0x72);
            rb.writeFloat((Float) v);
        } else if (v instanceof Double) {
            rb.write((byte) 0x82);
            rb.writeDouble((Double) v);
        } else if (v instanceof AmqpTimestamp) {
            rb.write((byte) 0x83);
            rb.writeLong(((AmqpTimestamp) v).getMillis());
        } else if (v instanceof UUID) {
            rb.write((byte) 0x98);
            rb.writeLong(((UUID) v).getMostSignificantBits());
            rb.writeLong(((UUID) v).getLeastSignificantBits());
        } else if (v instanceof AmqpChar) {
            rb.write((byte) 0x73);
            rb.writeInt(((AmqpChar) v).getCodePoint());
        } else if (v instanceof String) {
            writeVariable(rb, ((String) v).getBytes(StandardCharsets.UTF_8), (byte) 0xA1, (byte) 0xB1);
        } else if (v instanceof AmqpSymbol) {
            writeVariable(rb, ((AmqpSymbol) v).getValue().getBytes(StandardCharsets.US_ASCII), (byte) 0xA3, (byte) 0xB3);
        } else if (v instanceof Amqp10Binary) {
            writeVariable(rb, ((Amqp10Binary) v).getValue(), (byte) 0xA0, (byte) 0xB0);
        } else if (v instanceof List) {
            writeList(rb, (List<?>) v);
        } else if (v instanceof Map) {
            writeMap(rb, (Map<?, ?>) v);
        } else if (v instanceof DescribedType) {
            var dt = (DescribedType) v;
            rb.write((byte) 0x00);
            writeAny(rb, dt.getDescriptor());
            writeAny(rb, dt.getValue());
        } else {
            throw new TPMProtocolException("Amqp10TypeWriter: cannot encode " + v.getClass().getName());
        }
    }

    /** Encodes a performative/section: described type over a field list, trailing nulls dropped. */
    public void writeDescribed(BBuffer rb, long code, List<Object> fields) {
        rb.write((byte) 0x00);
        if (Long.compareUnsigned(code, 0xFF) <= 0) {
            rb.write((byte) 0x53);
            rb.write((byte) code);
        } else {
            rb.write((byte) 0x80);
            rb.writeLong(code);
        }
        int last = fields.size();
        while (last > 0 && fields.get(last - 1) == null) {
            last--;
        }
        writeList(rb, fields.subList(0, last));
    }

    private void writeVariable(BBuffer rb, byte[] bytes, byte code8, byte code32) {
        if (bytes.length <= 0xFF) {
            rb.write(code8);
            rb.write((byte) bytes.length);
        } else {
            rb.write(code32);
            rb.writeInt(bytes.length);
        }
        rb.write(bytes);
    }

    private void writeList(BBuffer rb, List<?> list) {
        if (list.isEmpty()) {
            rb.write((byte) 0x45);
            return;
        }
        var tmp = new BBuffer();
        for (var e : list) {
            writeAny(tmp, e);
        }
        var body = tmp.getAll();
        int count = list.size();
        if (count <= 0xFF && body.length + 1 <= 0xFF) {
            rb.write((byte) 0xC0);
            rb.write((byte) (body.length + 1));
            rb.write((byte) count);
        } else {
            rb.write((byte) 0xD0);
            rb.writeInt(body.length + 4);
            rb.writeInt(count);
        }
        rb.write(body);
    }

    private void writeMap(BBuffer rb, Map<?, ?> map) {
        var tmp = new BBuffer();
        for (var e : map.entrySet()) {
            writeAny(tmp, e.getKey());
            writeAny(tmp, e.getValue());
        }
        var body = tmp.getAll();
        int count = map.size() * 2;
        if (count <= 0xFF && body.length + 1 <= 0xFF) {
            rb.write((byte) 0xC1);
            rb.write((byte) (body.length + 1));
            rb.write((byte) count);
        } else {
            rb.write((byte) 0xD1);
            rb.writeInt(body.length + 4);
            rb.writeInt(count);
        }
        rb.write(body);
    }
}
