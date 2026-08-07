package org.kendar.amqp.v10;

import org.junit.jupiter.api.Test;
import org.kendar.amqp.v10.codec.Amqp10Binary;
import org.kendar.amqp.v10.codec.Amqp10FrameDescriber;
import org.kendar.amqp.v10.codec.Amqp10FrameEncoder;
import org.kendar.amqp.v10.codec.Amqp10Frames;
import org.kendar.amqp.v10.codec.Amqp10TypeWriter;
import org.kendar.amqp.v10.codec.DescribedType;
import org.kendar.amqp.v10.codec.UnsignedInt;
import org.kendar.amqp.v10.codec.UnsignedLong;
import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.RawFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.utils.JsonMapper;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M5 gate: raw frames decode to a readable tree ({@code decoded} property of
 * recordings). No broker needed. The decoded view is informational — replay
 * reads only {@code raw} — so these tests never re-encode from it.
 */
class FrameDescribeTest {
    private final Amqp10TypeWriter writer = new Amqp10TypeWriter();
    private final JsonMapper mapper = new JsonMapper();

    private byte[] frameOf(short channel, byte type, long code, List<Object> fields) {
        var body = new BBuffer();
        writer.writeDescribed(body, code, fields);
        return Amqp10Frames.frame(channel, type, body.getAll());
    }

    @Test
    void openFrame() {
        var raw = frameOf((short) 0, (byte) 0, Performatives.OPEN,
                Arrays.asList("container-1", "localhost", UnsignedInt.of(65536)));
        var d = Amqp10FrameDescriber.describe(raw);
        assertNotNull(d);
        assertEquals("AMQP", d.get("frameKind"));
        assertEquals(0, d.get("channel"));
        assertEquals("open", d.get("performative"));
        @SuppressWarnings("unchecked")
        var fields = (Map<String, Object>) d.get("fields");
        assertEquals("container-1", fields.get("container-id"));
        assertEquals("localhost", fields.get("hostname"));
        assertEquals(65536L, fields.get("max-frame-size"));
    }

    @Test
    void attachFrameWithNestedTarget() {
        var target = new DescribedType(UnsignedLong.of(0x29),
                Arrays.asList("my-queue", UnsignedInt.of(0)));
        var raw = frameOf((short) 1, (byte) 0, Performatives.ATTACH,
                Arrays.asList("link-1", UnsignedInt.of(0), Boolean.FALSE, null, null, null, target));
        var d = Amqp10FrameDescriber.describe(raw);
        assertEquals("attach", d.get("performative"));
        @SuppressWarnings("unchecked")
        var fields = (Map<String, Object>) d.get("fields");
        assertEquals("link-1", fields.get("name"));
        assertEquals(Boolean.FALSE, fields.get("role"));
        assertFalse(fields.containsKey("snd-settle-mode")); // null → omitted
        @SuppressWarnings("unchecked")
        var tgt = (Map<String, Object>) fields.get("target");
        assertEquals("target", tgt.get("type"));
        @SuppressWarnings("unchecked")
        var tgtFields = (Map<String, Object>) tgt.get("fields");
        assertEquals("my-queue", tgtFields.get("address"));
    }

    @Test
    void transferFrameWithSections() {
        var props = new LinkedHashMap<Object, Object>();
        props.put("k", "v");
        // application-properties value is a map, not a list: write it manually
        var appProps = new BBuffer();
        appProps.write((byte) 0x00);
        writer.writeAny(appProps, UnsignedLong.of(Performatives.APPLICATION_PROPERTIES));
        writer.writeAny(appProps, props);
        var data = new BBuffer();
        data.write((byte) 0x00);
        writer.writeAny(data, UnsignedLong.of(Performatives.DATA));
        writer.writeAny(data, new Amqp10Binary("hello world".getBytes()));

        var full = new BBuffer();
        writer.writeDescribed(full, Performatives.TRANSFER,
                Arrays.asList(UnsignedInt.of(0), UnsignedInt.of(1),
                        new Amqp10Binary(new byte[]{0}), UnsignedInt.of(0), Boolean.TRUE));
        full.write(appProps.getAll());
        full.write(data.getAll());
        var raw = Amqp10Frames.frame((short) 1, (byte) 0, full.getAll());

        var d = Amqp10FrameDescriber.describe(raw);
        assertEquals("transfer", d.get("performative"));
        @SuppressWarnings("unchecked")
        var fields = (Map<String, Object>) d.get("fields");
        assertEquals(1L, fields.get("delivery-id"));
        @SuppressWarnings("unchecked")
        var sections = (List<Map<String, Object>>) d.get("sections");
        assertEquals(2, sections.size());
        assertEquals("application-properties", sections.get(0).get("section"));
        assertEquals(Map.of("k", "v"), sections.get(0).get("value"));
        assertEquals("data", sections.get(1).get("section"));
        assertEquals("hello world", sections.get(1).get("utf8"));
        assertEquals(Base64.getEncoder().encodeToString("hello world".getBytes()),
                sections.get(1).get("base64"));
    }

    @Test
    void committedScenarioSaslOutcome() {
        // Raw payload from test/resources/replay_open/scenario/0000000005.default.json
        var raw = Base64.getDecoder().decode("AAAAEAIBAAAAU0TAAwFQAA==");
        var d = Amqp10FrameDescriber.describe(raw);
        assertEquals("SASL", d.get("frameKind"));
        assertEquals("sasl-outcome", d.get("performative"));
        @SuppressWarnings("unchecked")
        var fields = (Map<String, Object>) d.get("fields");
        assertEquals(0L, fields.get("code")); // sasl ok
    }

    @Test
    void protocolHeaderAndEmptyFrame() {
        var header = new byte[]{'A', 'M', 'Q', 'P', 3, 1, 0, 0};
        var d = Amqp10FrameDescriber.describe(header);
        assertEquals("protocol-header", d.get("performative"));
        assertEquals("SASL", d.get("layer"));
        assertEquals("1.0.0", d.get("version"));

        var empty = Amqp10Frames.frame((short) 0, (byte) 0, new byte[0]);
        var e = Amqp10FrameDescriber.describe(empty);
        assertEquals("empty", e.get("performative"));
    }

    @Test
    void totalOnGarbage() {
        assertNull(Amqp10FrameDescriber.describe(null));
        assertNull(Amqp10FrameDescriber.describe(new byte[]{1, 2, 3}));
        // Valid envelope, garbage body: must not throw
        var garbage = Amqp10Frames.frame((short) 0, (byte) 0, new byte[]{(byte) 0xFF, (byte) 0x91});
        assertDoesNotThrow(() -> Amqp10FrameDescriber.describe(garbage));
    }

    @Test
    void roundTrippableFrameSerializesWithoutRaw() {
        var raw = frameOf((short) 0, (byte) 0, Performatives.OPEN, List.of("cid"));
        var frame = new RawFrame(Performatives.OPEN, (byte) 0);
        frame.setChannel((short) 0);
        frame.setRaw(raw);
        var json = mapper.serialize(frame);
        var node = mapper.toJsonNode(json);
        // readable view is the stored representation…
        assertEquals("open", node.get("decoded").get("performative").asText());
        assertEquals("cid", node.get("decoded").get("fields").get("container-id").asText());
        // …and the raw fallback is dropped because decoded round-trips
        assertNull(node.get("raw"));
        // the encoder rebuilds a semantically identical frame from the JSON tree
        @SuppressWarnings("unchecked")
        Map<String, Object> decoded = mapper.deserialize(node.get("decoded"), Map.class);
        var rebuilt = Amqp10FrameEncoder.encode(decoded);
        assertEquals(Amqp10FrameDescriber.describe(raw), Amqp10FrameDescriber.describe(rebuilt));
    }

    @Test
    void undecodableFrameKeepsRawFallback() {
        var raw = Amqp10Frames.frame((short) 0, (byte) 0, new byte[]{(byte) 0xFF, (byte) 0x91});
        var frame = new RawFrame(-1, (byte) 0);
        frame.setRaw(raw);
        var node = mapper.toJsonNode(mapper.serialize(frame));
        assertArrayEquals(raw, Base64.getDecoder().decode(node.get("raw").asText()));
    }

    @Test
    void encoderRoundTripsEveryHandshakePerformative() {
        // Simulated client/broker handshake frames: describe → encode → describe is stable
        var frames = List.of(
                new byte[]{'A', 'M', 'Q', 'P', 3, 1, 0, 0},
                Base64.getDecoder().decode("AAAAEAIBAAAAU0TAAwFQAA=="), // recorded sasl-outcome
                frameOf((short) 0, (byte) 0, Performatives.OPEN,
                        Arrays.asList("cid", "host", UnsignedInt.of(65536), null, UnsignedInt.of(30000))),
                frameOf((short) 0, (byte) 0, Performatives.BEGIN,
                        Arrays.asList(null, UnsignedInt.of(1), UnsignedInt.of(5000), UnsignedInt.of(5000))),
                frameOf((short) 0, (byte) 0, Performatives.ATTACH,
                        Arrays.asList("l", UnsignedInt.of(0), Boolean.TRUE, null, null,
                                new DescribedType(UnsignedLong.of(0x28), List.of("q")),
                                new DescribedType(UnsignedLong.of(0x29), List.of("q")))),
                frameOf((short) 0, (byte) 0, Performatives.FLOW,
                        Arrays.asList(UnsignedInt.of(0), UnsignedInt.of(100), UnsignedInt.of(0),
                                UnsignedInt.of(100), UnsignedInt.of(0), UnsignedInt.of(0), UnsignedInt.of(500))),
                frameOf((short) 0, (byte) 0, Performatives.DISPOSITION,
                        Arrays.asList(Boolean.TRUE, UnsignedInt.of(0), UnsignedInt.of(0), Boolean.TRUE,
                                new DescribedType(UnsignedLong.of(0x24), List.of()))),
                frameOf((short) 0, (byte) 0, Performatives.DETACH,
                        Arrays.asList(UnsignedInt.of(0), Boolean.TRUE)),
                frameOf((short) 0, (byte) 0, Performatives.END, List.of()),
                frameOf((short) 0, (byte) 0, Performatives.CLOSE, List.of()),
                Amqp10Frames.frame((short) 0, (byte) 0, new byte[0])); // heartbeat
        for (var raw : frames) {
            var decoded = Amqp10FrameDescriber.describe(raw);
            @SuppressWarnings("unchecked")
            Map<String, Object> canonical = mapper.deserialize(mapper.serialize(decoded), Map.class);
            var rebuilt = Amqp10FrameEncoder.encode(canonical);
            assertEquals(decoded, Amqp10FrameDescriber.describe(rebuilt),
                    "round-trip failed for " + decoded.get("performative"));
        }
    }
}
