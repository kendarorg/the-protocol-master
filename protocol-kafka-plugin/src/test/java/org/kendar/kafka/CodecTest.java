package org.kendar.kafka;

import org.junit.jupiter.api.Test;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.enums.KafkaApiKeys;
import org.kendar.kafka.fsm.ApiVersionsResponse;
import org.kendar.kafka.fsm.MetadataResponse;
import org.kendar.kafka.utils.KafkaBBuffer;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Codec round-trips and the two semantic transforms (ApiVersions cap, Metadata
 * address rewrite) — no broker required (M2 gate).
 */
public class CodecTest {

    @Test
    void unsignedVarintRoundTrip() {
        for (int v : new int[]{0, 1, 127, 128, 300, 16384, 2097151, Integer.MAX_VALUE}) {
            var b = new KafkaBBuffer();
            b.writeUnsignedVarint(v);
            b.setPosition(0);
            assertEquals(v, b.readUnsignedVarint());
        }
    }

    @Test
    void signedVarintZigzagRoundTrip() {
        for (int v : new int[]{0, -1, 1, -64, 63, -1000, 1000, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            var b = new KafkaBBuffer();
            b.writeVarint(v);
            b.setPosition(0);
            assertEquals(v, b.readVarint());
        }
    }

    @Test
    void compactAndClassicStrings() {
        var b = new KafkaBBuffer();
        b.writeCompactString("hello");
        b.writeCompactString(null);
        b.writeString("world");
        b.writeString(null);
        b.setPosition(0);
        assertEquals("hello", b.readCompactString());
        assertNull(b.readCompactString());
        assertEquals("world", b.readString());
        assertNull(b.readString());
    }

    @Test
    void taggedFieldsAreOpaque() {
        var b = new KafkaBBuffer();
        // one tagged field: tag=5, size=3, value=abc
        b.writeUnsignedVarint(1);
        b.writeUnsignedVarint(5);
        b.writeUnsignedVarint(3);
        b.write("abc".getBytes(StandardCharsets.UTF_8));
        var expected = b.getAll();
        b.setPosition(0);
        assertArrayEquals(expected, b.readTaggedFieldsRaw());
    }

    @Test
    void apiVersionsResponseIsCapped() {
        // ApiVersions v3 (flexible) response: cap Metadata(3) 20 -> 12, Fetch(1) 16 -> 12,
        // ApiVersions(18) 3 stays 3 (uncapped).
        var body = new KafkaBBuffer();
        body.writeInt(42);                      // correlation id (header v0)
        body.writeShort((short) 0);             // error_code
        body.writeUnsignedVarint(4);            // compact array count = 3 entries
        writeApiEntry(body, KafkaApiKeys.METADATA, (short) 0, (short) 20);
        writeApiEntry(body, KafkaApiKeys.FETCH, (short) 0, (short) 16);
        writeApiEntry(body, KafkaApiKeys.API_VERSIONS, (short) 0, (short) 3);
        body.writeInt(0);                       // throttle
        body.writeUnsignedVarint(0);            // top-level tagged fields
        var frame = withSize(body.getAll());

        var out = new ApiVersionsResponse(42, (short) 3).rewrite(frame, null);

        var kb = new KafkaBBuffer(out);
        kb.getInt();                            // size
        kb.getInt();                            // corr
        kb.getShort();                          // error
        int count = kb.readUnsignedVarint() - 1;
        assertEquals(3, count);
        assertEquals(12, entryMax(kb));         // Metadata capped
        assertEquals(12, entryMax(kb));         // Fetch capped
        assertEquals(3, entryMax(kb));          // ApiVersions unchanged
    }

    @Test
    void metadataResponseRewritesBrokers() {
        // Metadata v12 (flexible) response, one broker at realbroker:9999, then a
        // verbatim tail that must survive unchanged.
        short version = 12;
        var tail = "TAIL-cluster-and-topics".getBytes(StandardCharsets.UTF_8);
        var body = new KafkaBBuffer();
        body.writeInt(7);                       // correlation id
        body.writeUnsignedVarint(0);            // header tagged fields (v1)
        body.writeInt(0);                       // throttle (v>=3)
        body.writeUnsignedVarint(2);            // brokers: 1
        body.writeInt(11);                      // node_id
        body.writeCompactString("realbroker");  // host
        body.writeInt(9999);                    // port
        body.writeCompactString(null);          // rack
        body.writeUnsignedVarint(0);            // broker tagged fields
        body.write(tail);
        var frame = withSize(body.getAll());

        var proto = new KafkaProtocol(9192);    // advertisedHost=localhost, port=9192
        proto.initialize();
        var ctx = new KafkaContext(proto, 1);
        var out = new MetadataResponse(7, version).rewrite(frame, ctx);

        var kb = new KafkaBBuffer(out);
        kb.getInt();                            // size
        assertEquals(7, kb.getInt());           // corr preserved
        kb.readTaggedFieldsRaw();               // header tagged
        assertEquals(0, kb.getInt());           // throttle
        assertEquals(1, kb.readUnsignedVarint() - 1);
        assertEquals(11, kb.getInt());          // node_id preserved
        assertEquals("localhost", kb.readCompactString());
        assertEquals(9192, kb.getInt());
        assertNull(kb.readCompactString());     // rack
        kb.readTaggedFieldsRaw();
        assertArrayEquals(tail, kb.getBytes(kb.size() - kb.getPosition())); // tail verbatim
    }

    private static void writeApiEntry(KafkaBBuffer b, short apiKey, short min, short max) {
        b.writeShort(apiKey);
        b.writeShort(min);
        b.writeShort(max);
        b.writeUnsignedVarint(0);               // entry tagged fields
    }

    private static short entryMax(KafkaBBuffer kb) {
        kb.getShort();                          // api_key
        kb.getShort();                          // min
        short max = kb.getShort();
        kb.readTaggedFieldsRaw();
        return max;
    }

    private static byte[] withSize(byte[] body) {
        var out = new KafkaBBuffer();
        out.writeInt(body.length);
        out.write(body);
        return out.getAll();
    }
}
