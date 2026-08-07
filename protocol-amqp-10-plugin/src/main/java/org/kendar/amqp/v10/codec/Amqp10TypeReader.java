package org.kendar.amqp.v10.codec;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.TPMProtocolException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads AMQP 1.0 self-describing values from a big-endian {@link BBuffer}.
 * {@link #readAny(BBuffer)} switches on the format code (§1.6 / §5.6.4) and
 * recurses through described types, compounds and arrays. Ambiguous Java types
 * are wrapped ({@link AmqpSymbol}, {@link UnsignedByte}/{@link UnsignedShort}/
 * {@link UnsignedInt}/{@link UnsignedLong}, {@link Amqp10Binary},
 * {@link AmqpTimestamp}, {@link AmqpChar}) so a value round-trips to the same type.
 */
public class Amqp10TypeReader {

    public Object readAny(BBuffer rb) {
        int code = rb.get() & 0xFF;
        switch (code) {
            // constants
            case 0x40:
                return null;
            case 0x41:
                return Boolean.TRUE;
            case 0x42:
                return Boolean.FALSE;
            case 0x43:
                return UnsignedInt.of(0);
            case 0x44:
                return UnsignedLong.of(0);
            case 0x45:
                return new ArrayList<>(); // list0
            // fixed one
            case 0x50:
                return UnsignedByte.of(rb.get() & 0xFF);
            case 0x51:
                return rb.get();
            case 0x52:
                return UnsignedInt.of(rb.get() & 0xFFL); // smalluint
            case 0x53:
                return UnsignedLong.of(rb.get() & 0xFFL); // smallulong
            case 0x54:
                return (int) rb.get(); // smallint
            case 0x55:
                return (long) rb.get(); // smalllong
            case 0x56:
                return rb.get() != 0; // boolean
            // fixed two
            case 0x60:
                return UnsignedShort.of(rb.getShort() & 0xFFFF);
            case 0x61:
                return rb.getShort();
            // fixed four
            case 0x70:
                return UnsignedInt.of(rb.getInt() & 0xFFFFFFFFL);
            case 0x71:
                return rb.getInt();
            case 0x72:
                return ByteBuffer.wrap(rb.getBytes(4)).getFloat();
            case 0x73:
                return new AmqpChar(rb.getInt());
            case 0x74:
                return new Amqp10Binary(rb.getBytes(4)); // decimal32 (raw)
            // fixed eight
            case 0x80:
                return new UnsignedLong(rb.getLong());
            case 0x81:
                return rb.getLong();
            case 0x82:
                return ByteBuffer.wrap(rb.getBytes(8)).getDouble();
            case 0x83:
                return new AmqpTimestamp(rb.getLong());
            case 0x84:
                return new Amqp10Binary(rb.getBytes(8)); // decimal64 (raw)
            // fixed sixteen
            case 0x94:
                return new Amqp10Binary(rb.getBytes(16)); // decimal128 (raw)
            case 0x98: {
                var b = rb.getBytes(16);
                var bb = ByteBuffer.wrap(b);
                return new UUID(bb.getLong(), bb.getLong());
            }
            // variable one
            case 0xA0:
                return new Amqp10Binary(rb.getBytes(rb.get() & 0xFF));
            case 0xA1:
                return new String(rb.getBytes(rb.get() & 0xFF), java.nio.charset.StandardCharsets.UTF_8);
            case 0xA3:
                return new AmqpSymbol(new String(rb.getBytes(rb.get() & 0xFF), java.nio.charset.StandardCharsets.US_ASCII));
            // variable four
            case 0xB0:
                return new Amqp10Binary(rb.getBytes(rb.getInt()));
            case 0xB1:
                return new String(rb.getBytes(rb.getInt()), java.nio.charset.StandardCharsets.UTF_8);
            case 0xB3:
                return new AmqpSymbol(new String(rb.getBytes(rb.getInt()), java.nio.charset.StandardCharsets.US_ASCII));
            // compound
            case 0xC0:
                return readList(rb, rb.get() & 0xFF, rb.get() & 0xFF);
            case 0xC1:
                return readMap(rb, rb.get() & 0xFF, rb.get() & 0xFF);
            case 0xD0:
                return readList(rb, rb.getInt(), rb.getInt());
            case 0xD1:
                return readMap(rb, rb.getInt(), rb.getInt());
            // arrays
            case 0xE0:
                return readArray(rb, rb.get() & 0xFF, rb.get() & 0xFF);
            case 0xF0:
                return readArray(rb, rb.getInt(), rb.getInt());
            // described
            case 0x00: {
                var descriptor = readAny(rb);
                var value = readAny(rb);
                return new DescribedType(descriptor, value);
            }
            default:
                throw new TPMProtocolException("Amqp10TypeReader: unknown format code 0x"
                        + Integer.toHexString(code));
        }
    }

    private List<Object> readList(BBuffer rb, int size, int count) {
        // size is bytes after the size field; we drive off count instead
        var list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(readAny(rb));
        }
        return list;
    }

    private Map<Object, Object> readMap(BBuffer rb, int size, int count) {
        var map = new LinkedHashMap<>();
        for (int i = 0; i < count / 2; i++) {
            var k = readAny(rb);
            var v = readAny(rb);
            map.put(k, v);
        }
        return map;
    }

    /** Arrays share a single element constructor (format code) applied to all elements. */
    private List<Object> readArray(BBuffer rb, int size, int count) {
        int elementCode = rb.get() & 0xFF;
        var list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(readArrayElement(rb, elementCode));
        }
        return list;
    }

    private Object readArrayElement(BBuffer rb, int code) {
        switch (code) {
            case 0x41:
                return Boolean.TRUE;
            case 0x42:
                return Boolean.FALSE;
            case 0x50:
                return UnsignedByte.of(rb.get() & 0xFF);
            case 0x51:
                return rb.get();
            case 0x56:
                return rb.get() != 0;
            case 0x60:
                return UnsignedShort.of(rb.getShort() & 0xFFFF);
            case 0x61:
                return rb.getShort();
            case 0x70:
                return UnsignedInt.of(rb.getInt() & 0xFFFFFFFFL);
            case 0x71:
                return rb.getInt();
            case 0x72:
                return ByteBuffer.wrap(rb.getBytes(4)).getFloat();
            case 0x80:
                return new UnsignedLong(rb.getLong());
            case 0x81:
                return rb.getLong();
            case 0x82:
                return ByteBuffer.wrap(rb.getBytes(8)).getDouble();
            case 0x83:
                return new AmqpTimestamp(rb.getLong());
            case 0x98: {
                var bb = ByteBuffer.wrap(rb.getBytes(16));
                return new UUID(bb.getLong(), bb.getLong());
            }
            case 0xA1:
                return new String(rb.getBytes(rb.get() & 0xFF), java.nio.charset.StandardCharsets.UTF_8);
            case 0xA3:
                return new AmqpSymbol(new String(rb.getBytes(rb.get() & 0xFF), java.nio.charset.StandardCharsets.US_ASCII));
            case 0xB1:
                return new String(rb.getBytes(rb.getInt()), java.nio.charset.StandardCharsets.UTF_8);
            case 0xB3:
                return new AmqpSymbol(new String(rb.getBytes(rb.getInt()), java.nio.charset.StandardCharsets.US_ASCII));
            case 0x00: {
                // described array: descriptor once, then values share the value constructor
                var descriptor = readAny(rb);
                int valueCode = rb.get() & 0xFF;
                return new DescribedType(descriptor, readArrayElement(rb, valueCode));
            }
            default:
                throw new TPMProtocolException("Amqp10TypeReader: unsupported array element code 0x"
                        + Integer.toHexString(code));
        }
    }
}
