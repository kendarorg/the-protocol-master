package org.kendar.amqp.v10.codec;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.TPMProtocolException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inverse of {@link Amqp10FrameDescriber}: rebuilds a wire frame from the
 * readable {@code decoded} tree stored in recordings. Field wire types come from
 * the shared {@link Amqp10Schema}; untyped values arrive either as JSON-native
 * scalars or as the describer's {@code {"type": ..., "value": ...}} wrappers.
 * <p>
 * The re-encoded bytes are semantically equivalent but not byte-identical to the
 * original (smallest-encoding choices differ), so recordings only drop {@code raw}
 * when {@code describe(encode(decoded))} round-trips — see
 * {@code Amqp10BaseFrame#getRawBase64()}. Unknown descriptors or sections throw,
 * which fails that gate and keeps the raw fallback.
 */
public final class Amqp10FrameEncoder {

    private Amqp10FrameEncoder() {
    }

    /**
     * Rebuilds the full wire frame (envelope included) from a {@code decoded} tree
     * as produced by {@link Amqp10FrameDescriber#describe}. Throws on anything the
     * schema does not cover — callers treat that as "keep the raw fallback".
     */
    @SuppressWarnings("unchecked")
    public static byte[] encode(Map<String, Object> decoded) {
        var performative = (String) decoded.get("performative");
        if (performative == null) {
            throw new TPMProtocolException("Amqp10FrameEncoder: no performative");
        }
        if ("protocol-header".equals(performative)) {
            var layer = String.valueOf(decoded.get("layer"));
            var version = String.valueOf(decoded.getOrDefault("version", "1.0.0")).split("\\.");
            return new byte[]{'A', 'M', 'Q', 'P',
                    "SASL".equals(layer) ? 3 : (byte) 0,
                    (byte) Integer.parseInt(version[0]),
                    (byte) Integer.parseInt(version[1]),
                    (byte) Integer.parseInt(version[2])};
        }
        var channel = (short) asLong(decoded.getOrDefault("channel", 0));
        var kind = "SASL".equals(decoded.get("frameKind")) ? (byte) 1 : (byte) 0;
        if ("empty".equals(performative)) {
            return Amqp10Frames.frame(channel, kind, new byte[0]);
        }
        var spec = Amqp10Schema.byName(performative);
        if (spec == null) {
            throw new TPMProtocolException("Amqp10FrameEncoder: unknown performative " + performative);
        }
        var writer = new Amqp10TypeWriter();
        var body = new BBuffer();
        writer.writeDescribed(body, spec.code,
                positional(spec, (Map<String, Object>) decoded.getOrDefault("fields", Map.of())));
        var sections = (List<Map<String, Object>>) decoded.get("sections");
        if (sections != null) {
            for (var section : sections) {
                encodeSection(writer, body, section);
            }
        }
        return Amqp10Frames.frame(channel, kind, body.getAll());
    }

    @SuppressWarnings("unchecked")
    private static void encodeSection(Amqp10TypeWriter writer, BBuffer body, Map<String, Object> section) {
        var name = (String) section.get("section");
        if (name == null) {
            throw new TPMProtocolException("Amqp10FrameEncoder: section without name");
        }
        switch (name) {
            case "data":
                describedValue(writer, body, 0x75,
                        new Amqp10Binary(Base64.getDecoder().decode((String) section.get("base64"))));
                return;
            case "amqp-value":
                describedValue(writer, body, 0x77, any(section.get("value")));
                return;
            case "amqp-sequence":
                describedValue(writer, body, 0x76, any(section.get("value")));
                return;
            case "delivery-annotations":
                describedValue(writer, body, 0x71, symbolMap(section.get("value")));
                return;
            case "message-annotations":
                describedValue(writer, body, 0x72, symbolMap(section.get("value")));
                return;
            case "footer":
                describedValue(writer, body, 0x78, symbolMap(section.get("value")));
                return;
            case "application-properties":
                describedValue(writer, body, 0x74, anyMap(section.get("value")));
                return;
            case "header":
            case "properties": {
                var spec = Amqp10Schema.byName(name);
                writer.writeDescribed(body, spec.code,
                        positional(spec, (Map<String, Object>) section.getOrDefault("fields", Map.of())));
                return;
            }
            default:
                throw new TPMProtocolException("Amqp10FrameEncoder: unknown section " + name);
        }
    }

    private static void describedValue(Amqp10TypeWriter writer, BBuffer body, long code, Object value) {
        body.write((byte) 0x00);
        writer.writeAny(body, UnsignedLong.of(code));
        writer.writeAny(body, value);
    }

    private static List<Object> positional(Amqp10Schema.Spec spec, Map<String, Object> fields) {
        var out = new ArrayList<>();
        for (int i = 0; i < spec.names.length; i++) {
            out.add(coerce(spec.types[i], fields.get(spec.names[i])));
        }
        for (var key : fields.keySet()) {
            if (key.startsWith("field-")) {
                var index = Integer.parseInt(key.substring("field-".length()));
                while (out.size() <= index) {
                    out.add(null);
                }
                out.set(index, any(fields.get(key)));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object coerce(Amqp10Schema.FieldType type, Object v) {
        if (v == null) {
            return null;
        }
        switch (type) {
            case UINT:
                return UnsignedInt.of(asLong(v));
            case ULONG:
                return UnsignedLong.of(asLong(v));
            case UBYTE:
                return UnsignedByte.of((int) asLong(v));
            case USHORT:
                return UnsignedShort.of((int) asLong(v));
            case BOOL:
                return v;
            case STR:
                return String.valueOf(v);
            case SYM:
                return new AmqpSymbol(String.valueOf(v));
            case BIN:
                return new Amqp10Binary(Base64.getDecoder().decode(String.valueOf(v)));
            case TS:
                return new AmqpTimestamp(asLong(v));
            case MULTI_SYM:
                if (v instanceof List) {
                    return symbolArray((List<Object>) v);
                }
                return new AmqpSymbol(String.valueOf(v));
            case MAP_SYM:
                return symbolMap(v);
            case MAP_STR:
            case MAP_ANY:
                return anyMap(v);
            case DESCRIBED:
                return described((Map<String, Object>) v);
            case ANY:
            default:
                return any(v);
        }
    }

    @SuppressWarnings("unchecked")
    private static DescribedType described(Map<String, Object> v) {
        var name = (String) v.get("type");
        var spec = name == null ? null : Amqp10Schema.byName(name);
        if (spec == null) {
            throw new TPMProtocolException("Amqp10FrameEncoder: unknown described type " + v);
        }
        var fields = positional(spec, (Map<String, Object>) v.getOrDefault("fields", Map.of()));
        while (!fields.isEmpty() && fields.get(fields.size() - 1) == null) {
            fields.remove(fields.size() - 1);
        }
        return new DescribedType(UnsignedLong.of(spec.code), fields);
    }

    private static Map<Object, Object> symbolMap(Object v) {
        var out = new LinkedHashMap<>();
        if (v instanceof Map) {
            for (var e : ((Map<?, ?>) v).entrySet()) {
                out.put(new AmqpSymbol(String.valueOf(e.getKey())), any(e.getValue()));
            }
        }
        return out;
    }

    private static Map<Object, Object> anyMap(Object v) {
        var out = new LinkedHashMap<>();
        if (v instanceof Map) {
            for (var e : ((Map<?, ?>) v).entrySet()) {
                out.put(any(e.getKey()), any(e.getValue()));
            }
        }
        return out;
    }

    /**
     * Encodes an untyped value: JSON-native scalars pass through; the describer's
     * {@code {"type": ..., "value": ...}} wrappers restore the exact wire type;
     * maps with a schema {@code type} become nested described types.
     */
    @SuppressWarnings("unchecked")
    private static Object any(Object v) {
        if (v instanceof Map) {
            var map = (Map<String, Object>) v;
            var type = map.get("type");
            if (type instanceof String) {
                var unwrapped = unwrapScalar((String) type, map);
                if (unwrapped != UNKNOWN) {
                    return unwrapped;
                }
                return described(map);
            }
            return anyMap(map);
        }
        if (v instanceof List) {
            var out = new ArrayList<>();
            for (var e : (List<?>) v) {
                out.add(any(e));
            }
            return out;
        }
        return v;
    }

    private static final Object UNKNOWN = new Object();

    /** Restores a typed-wrapper scalar, or {@link #UNKNOWN} if the type is not a scalar wrapper. */
    private static Object unwrapScalar(String type, Map<String, Object> map) {
        var value = map.get("value");
        switch (type) {
            case "byte":
                return (byte) asLong(value);
            case "short":
                return (short) asLong(value);
            case "float":
                return ((Number) value).floatValue();
            case "char":
                return new AmqpChar((int) asLong(value));
            case "ubyte":
                return UnsignedByte.of((int) asLong(value));
            case "ushort":
                return UnsignedShort.of((int) asLong(value));
            case "uint":
                return UnsignedInt.of(asLong(value));
            case "ulong":
                return UnsignedLong.of(asLong(value));
            case "timestamp":
                return new AmqpTimestamp(asLong(value));
            case "uuid":
                return UUID.fromString(String.valueOf(value));
            case "symbol":
                return new AmqpSymbol(String.valueOf(value));
            case "binary":
                return new Amqp10Binary(Base64.getDecoder().decode((String) map.get("base64")));
            default:
                return UNKNOWN;
        }
    }

    private static long asLong(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        throw new TPMProtocolException("Amqp10FrameEncoder: expected number, got " + v);
    }

    /**
     * AMQP array of symbols ({@code multiple()} fields). The plain writer has no
     * array support (a list of symbols would be the wrong wire type), so build the
     * array8/array32 encoding here.
     */
    private static Object symbolArray(List<Object> symbols) {
        var wide = symbols.stream()
                .anyMatch(s -> String.valueOf(s).getBytes(StandardCharsets.US_ASCII).length > 0xFF);
        var elements = new BBuffer();
        for (var s : symbols) {
            var bytes = String.valueOf(s).getBytes(StandardCharsets.US_ASCII);
            elements.write(wide ? intBytes(bytes.length) : new byte[]{(byte) bytes.length});
            elements.write(bytes);
        }
        var body = elements.getAll();
        var out = new BBuffer();
        if (!wide && symbols.size() <= 0xFF && body.length + 2 <= 0xFF) {
            out.write((byte) 0xE0);
            out.write((byte) (body.length + 2));
            out.write((byte) symbols.size());
            out.write((byte) 0xA3);
        } else {
            out.write((byte) 0xF0);
            out.write(intBytes(body.length + 5));
            out.write(intBytes(symbols.size()));
            out.write((byte) 0xB3);
        }
        out.write(body);
        return new RawBytes(out.getAll());
    }

    private static byte[] intBytes(int v) {
        return new byte[]{(byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v};
    }

    /** Pre-encoded bytes spliced verbatim into a value stream (see {@link Amqp10TypeWriter}). */
    public static final class RawBytes {
        private final byte[] bytes;

        public RawBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
