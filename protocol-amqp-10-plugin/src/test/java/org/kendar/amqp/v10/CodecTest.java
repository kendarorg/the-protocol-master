package org.kendar.amqp.v10;

import org.junit.jupiter.api.Test;
import org.kendar.amqp.v10.codec.*;
import org.kendar.buffers.BBuffer;
import org.kendar.utils.JsonMapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure BBuffer round-trips for every AMQP 1.0 format code + Jackson JSON
 * round-trip of the wrapper types (M2 gate, no broker needed).
 */
class CodecTest {
    private final Amqp10TypeReader reader = new Amqp10TypeReader();
    private final Amqp10TypeWriter writer = new Amqp10TypeWriter();
    private final JsonMapper mapper = new JsonMapper();

    private Object roundTrip(Object value) {
        var bb = new BBuffer();
        writer.writeAny(bb, value);
        bb.setPosition(0);
        return reader.readAny(bb);
    }

    @Test
    void scalars() {
        assertNull(roundTrip(null));
        assertEquals(Boolean.TRUE, roundTrip(Boolean.TRUE));
        assertEquals(Boolean.FALSE, roundTrip(Boolean.FALSE));
        assertEquals((byte) -5, roundTrip((byte) -5));
        assertEquals((short) 12345, roundTrip((short) 12345));
        assertEquals(7, roundTrip(7));                       // smallint
        assertEquals(1_000_000, roundTrip(1_000_000));       // int
        assertEquals(9L, roundTrip(9L));                     // smalllong
        assertEquals(9_000_000_000L, roundTrip(9_000_000_000L));
        assertEquals(3.5f, roundTrip(3.5f));
        assertEquals(2.718281828d, roundTrip(2.718281828d));
    }

    @Test
    void unsignedTypes() {
        assertEquals(UnsignedByte.of(200), roundTrip(UnsignedByte.of(200)));
        assertEquals(UnsignedShort.of(40000), roundTrip(UnsignedShort.of(40000)));
        assertEquals(UnsignedInt.of(0), roundTrip(UnsignedInt.of(0)));       // uint0
        assertEquals(UnsignedInt.of(200), roundTrip(UnsignedInt.of(200)));   // smalluint
        assertEquals(UnsignedInt.of(70000), roundTrip(UnsignedInt.of(70000)));
        assertEquals(UnsignedLong.of(0), roundTrip(UnsignedLong.of(0)));     // ulong0
        assertEquals(UnsignedLong.of(255), roundTrip(UnsignedLong.of(255))); // smallulong
        assertEquals(UnsignedLong.of(0x1_0000_0000L), roundTrip(UnsignedLong.of(0x1_0000_0000L)));
    }

    @Test
    void specialScalars() {
        assertEquals(new AmqpTimestamp(1_700_000_000_000L), roundTrip(new AmqpTimestamp(1_700_000_000_000L)));
        var uuid = new UUID(0x0011223344556677L, 0x8899aabbccddeeffL);
        assertEquals(uuid, roundTrip(uuid));
        assertEquals(new AmqpChar(0x1F600), roundTrip(new AmqpChar(0x1F600)));
    }

    @Test
    void variableWidth() {
        assertEquals("hello", roundTrip("hello"));
        var big = "x".repeat(300);
        assertEquals(big, roundTrip(big));                                   // str32
        assertEquals(new AmqpSymbol("amqp:accepted:list"), roundTrip(new AmqpSymbol("amqp:accepted:list")));
        var bin = new Amqp10Binary(new byte[]{1, 2, 3, 4, 5});
        assertEquals(bin, roundTrip(bin));
    }

    @Test
    void compounds() {
        assertEquals(List.of(), roundTrip(List.of()));                       // list0
        var list = Arrays.asList("a", 1, UnsignedLong.of(9), null, Boolean.TRUE);
        assertEquals(list, roundTrip(list));
        var map = new LinkedHashMap<Object, Object>();
        map.put(new AmqpSymbol("key"), "value");
        map.put("n", 42);
        assertEquals(map, roundTrip(map));
    }

    @Test
    void describedTypeAndTruncation() {
        var dt = new DescribedType(UnsignedLong.of(0x10), Arrays.asList("container-id", "hostname"));
        assertEquals(dt, roundTrip(dt));

        // writeDescribed drops trailing nulls
        var bb = new BBuffer();
        writer.writeDescribed(bb, 0x10, Arrays.asList("id", null, null));
        bb.setPosition(0);
        var decoded = (DescribedType) reader.readAny(bb);
        assertEquals(0x10, decoded.descriptorCode());
        assertEquals(List.of("id"), decoded.getValue());
    }

    @Test
    void arrayReading() {
        // Hand-build sym8 array {"PLAIN","ANONYMOUS"} (used by sasl-mechanisms)
        var bb = new BBuffer();
        bb.write((byte) 0xE0);                 // array8
        var body = new BBuffer();
        body.write((byte) 0xA3);               // element constructor: sym8
        writeSym(body, "PLAIN");
        writeSym(body, "ANONYMOUS");
        var b = body.getAll();
        bb.write((byte) (b.length + 1));       // size
        bb.write((byte) 2);                    // count
        bb.write(b);
        bb.setPosition(0);
        var list = (List<?>) reader.readAny(bb);
        assertEquals(List.of(new AmqpSymbol("PLAIN"), new AmqpSymbol("ANONYMOUS")), list);
    }

    private static void writeSym(BBuffer bb, String s) {
        var by = s.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        bb.write((byte) by.length);
        bb.write(by);
    }

    @Test
    void jacksonJsonRoundTrip() {
        assertEquals(new AmqpSymbol("s"), mapper.deserialize(mapper.serialize(new AmqpSymbol("s")), AmqpSymbol.class));
        assertEquals(UnsignedByte.of(7), mapper.deserialize(mapper.serialize(UnsignedByte.of(7)), UnsignedByte.class));
        assertEquals(UnsignedShort.of(7), mapper.deserialize(mapper.serialize(UnsignedShort.of(7)), UnsignedShort.class));
        assertEquals(UnsignedInt.of(7), mapper.deserialize(mapper.serialize(UnsignedInt.of(7)), UnsignedInt.class));
        assertEquals(UnsignedLong.of(0x1_0000_0000L),
                mapper.deserialize(mapper.serialize(UnsignedLong.of(0x1_0000_0000L)), UnsignedLong.class));
        assertEquals(new AmqpTimestamp(123), mapper.deserialize(mapper.serialize(new AmqpTimestamp(123)), AmqpTimestamp.class));
        var bin = new Amqp10Binary(new byte[]{9, 8, 7});
        assertEquals(bin, mapper.deserialize(mapper.serialize(bin), Amqp10Binary.class));
    }
}
