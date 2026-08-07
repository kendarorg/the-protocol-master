package org.kendar.amqp.v10.codec;

import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.buffers.BBuffer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One-way decoder from a raw AMQP 1.0 frame to a human-readable, Jackson-friendly
 * tree (plain maps/lists/scalars). This tree is the primary representation stored
 * in recordings; {@link Amqp10FrameEncoder} rebuilds the wire frame from it.
 * <p>
 * Field names and wire types come from {@link Amqp10Schema}. Spec-typed fields
 * render as plain JSON scalars (the schema restores their wire type on encode).
 * Untyped contexts — map values, {@code amqp-value}/{@code amqp-sequence},
 * message-id/correlation-id — render ambiguous scalars as small typed wrappers
 * ({@code {"type": "byte", "value": 5}}) so the encoder can restore the exact
 * wire type; common types (string, boolean, int, long, double) stay plain.
 * <p>
 * The decoder is total: any malformed or unknown input yields {@code null} (or a
 * raw {@code descriptor/value} fallback for unknown described types), never an
 * exception.
 */
public final class Amqp10FrameDescriber {

    private Amqp10FrameDescriber() {
    }

    /**
     * Describes a raw AMQP 1.0 frame (or 8-byte protocol header) as a readable
     * tree, or {@code null} when the input cannot be decoded at all.
     */
    public static Map<String, Object> describe(byte[] frame) {
        try {
            if (frame == null || frame.length < 8) {
                return null;
            }
            if (frame[0] == 'A' && frame[1] == 'M' && frame[2] == 'Q' && frame[3] == 'P') {
                var out = new LinkedHashMap<String, Object>();
                out.put("performative", "protocol-header");
                out.put("layer", frame[4] == 3 ? "SASL" : frame[4] == 0 ? "AMQP" : "0x" + Integer.toHexString(frame[4] & 0xFF));
                out.put("version", (frame[5] & 0xFF) + "." + (frame[6] & 0xFF) + "." + (frame[7] & 0xFF));
                return out;
            }
            var out = new LinkedHashMap<String, Object>();
            out.put("frameKind", frame[5] == 1 ? "SASL" : "AMQP");
            out.put("channel", ((frame[6] & 0xFF) << 8) | (frame[7] & 0xFF));
            var bodyStart = (frame[4] & 0xFF) * 4;
            if (bodyStart < 8 || bodyStart >= frame.length) {
                out.put("performative", "empty");
                return out;
            }
            var bb = BBuffer.of(frame);
            bb.setPosition(bodyStart);
            var reader = new Amqp10TypeReader();
            var first = reader.readAny(bb);
            if (!(first instanceof DescribedType)) {
                out.put("body", renderAny(first));
                return out;
            }
            var dt = (DescribedType) first;
            var code = dt.descriptorCode();
            var spec = Amqp10Schema.byCode(code);
            out.put("performative", spec != null ? spec.name : "0x" + Long.toHexString(code));
            var fields = namedFields(spec, dt.getValue());
            if (fields != null && !fields.isEmpty()) {
                out.put("fields", fields);
            }
            // A transfer body carries the message sections after the performative.
            var sections = new ArrayList<>();
            while (bb.getPosition() < frame.length) {
                var pos = bb.getPosition();
                Object section;
                try {
                    section = reader.readAny(bb);
                } catch (Exception e) {
                    sections.add(base64(frame, pos));
                    break;
                }
                sections.add(section instanceof DescribedType
                        ? describeSection((DescribedType) section)
                        : renderAny(section));
            }
            if (!sections.isEmpty()) {
                out.put("sections", sections);
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    /** The performative name of a raw frame, or {@code null}. */
    public static String performativeName(byte[] frame) {
        var d = describe(frame);
        return d == null ? null : (String) d.get("performative");
    }

    private static Object describeSection(DescribedType dt) {
        var code = dt.descriptorCode();
        var spec = Amqp10Schema.byCode(code);
        var out = new LinkedHashMap<String, Object>();
        out.put("section", spec != null ? spec.name : "0x" + Long.toHexString(code));
        if (code == Performatives.DATA && dt.getValue() instanceof Amqp10Binary) {
            var bin = (Amqp10Binary) dt.getValue();
            out.put("base64", bin.toBase64());
            var utf8 = printable(bin.getValue());
            if (utf8 != null) {
                out.put("utf8", utf8);
            }
            return out;
        }
        if (spec != null && spec.names.length > 0) {
            var fields = namedFields(spec, dt.getValue());
            if (fields != null && !fields.isEmpty()) {
                out.put("fields", fields);
            }
        } else {
            var v = renderAny(dt.getValue());
            if (v != null) {
                out.put("value", v);
            }
        }
        return out;
    }

    /** Positional field list → named map (nulls omitted); non-list values render untyped. */
    private static Map<String, Object> namedFields(Amqp10Schema.Spec spec, Object value) {
        if (spec == null || !(value instanceof List)) {
            return null;
        }
        var list = (List<?>) value;
        var out = new LinkedHashMap<String, Object>();
        for (int i = 0; i < list.size(); i++) {
            var v = list.get(i);
            if (v == null) {
                continue;
            }
            if (i < spec.names.length) {
                out.put(spec.names[i], renderField(spec.types[i], v));
            } else {
                out.put("field-" + i, renderAny(v));
            }
        }
        return out;
    }

    private static Object renderField(Amqp10Schema.FieldType type, Object v) {
        switch (type) {
            case ANY:
                return renderAny(v);
            case MAP_SYM:
            case MAP_STR:
            case MAP_ANY:
                return renderMap(v);
            default:
                return render(v);
        }
    }

    /**
     * Plain rendering for spec-typed values: the schema knows the wire type, so a
     * bare JSON scalar is enough for the encoder to restore it.
     */
    private static Object render(Object v) {
        if (v instanceof AmqpSymbol) {
            return ((AmqpSymbol) v).getValue();
        }
        if (v instanceof UnsignedByte) {
            return (long) ((UnsignedByte) v).getValue();
        }
        if (v instanceof UnsignedShort) {
            return (long) ((UnsignedShort) v).getValue();
        }
        if (v instanceof UnsignedInt) {
            return ((UnsignedInt) v).getValue();
        }
        if (v instanceof UnsignedLong) {
            return ((UnsignedLong) v).getRawBits();
        }
        if (v instanceof AmqpTimestamp) {
            return ((AmqpTimestamp) v).getMillis();
        }
        if (v instanceof AmqpChar) {
            return ((AmqpChar) v).getCodePoint();
        }
        if (v instanceof Amqp10Binary) {
            return ((Amqp10Binary) v).toBase64();
        }
        if (v instanceof DescribedType) {
            return renderDescribed((DescribedType) v);
        }
        if (v instanceof List) {
            var out = new ArrayList<>();
            for (var e : (List<?>) v) {
                out.add(render(e));
            }
            return out;
        }
        if (v instanceof Map) {
            return renderMap(v);
        }
        return v; // null, Boolean, String, numbers, UUID (Jackson renders it as string)
    }

    /**
     * Typed rendering for untyped contexts: scalars whose wire type a bare JSON
     * number/string cannot express become {@code {"type": ..., "value": ...}}
     * wrappers. Common JSON-native types stay plain.
     */
    private static Object renderAny(Object v) {
        if (v == null || v instanceof Boolean || v instanceof String
                || v instanceof Integer || v instanceof Long || v instanceof Double) {
            return v;
        }
        if (v instanceof Byte) {
            return wrap("byte", ((Byte) v).intValue());
        }
        if (v instanceof Short) {
            return wrap("short", ((Short) v).intValue());
        }
        if (v instanceof Float) {
            return wrap("float", v);
        }
        if (v instanceof AmqpChar) {
            return wrap("char", ((AmqpChar) v).getCodePoint());
        }
        if (v instanceof UnsignedByte) {
            return wrap("ubyte", (int) ((UnsignedByte) v).getValue());
        }
        if (v instanceof UnsignedShort) {
            return wrap("ushort", ((UnsignedShort) v).getValue());
        }
        if (v instanceof UnsignedInt) {
            return wrap("uint", ((UnsignedInt) v).getValue());
        }
        if (v instanceof UnsignedLong) {
            return wrap("ulong", ((UnsignedLong) v).getRawBits());
        }
        if (v instanceof AmqpTimestamp) {
            return wrap("timestamp", ((AmqpTimestamp) v).getMillis());
        }
        if (v instanceof UUID) {
            return wrap("uuid", v.toString());
        }
        if (v instanceof AmqpSymbol) {
            return wrap("symbol", ((AmqpSymbol) v).getValue());
        }
        if (v instanceof Amqp10Binary) {
            var bin = (Amqp10Binary) v;
            var out = wrap("binary", null);
            out.remove("value");
            out.put("base64", bin.toBase64());
            var utf8 = printable(bin.getValue());
            if (utf8 != null) {
                out.put("utf8", utf8);
            }
            return out;
        }
        if (v instanceof DescribedType) {
            return renderDescribed((DescribedType) v);
        }
        if (v instanceof List) {
            var out = new ArrayList<>();
            for (var e : (List<?>) v) {
                out.add(renderAny(e));
            }
            return out;
        }
        if (v instanceof Map) {
            return renderMap(v);
        }
        return v;
    }

    /**
     * Map keys render as plain strings (symbol/string ambiguity is resolved by the
     * owning field's MAP_SYM/MAP_STR schema type); values are untyped.
     */
    private static Map<String, Object> renderMap(Object v) {
        var out = new LinkedHashMap<String, Object>();
        if (v instanceof Map) {
            for (var e : ((Map<?, ?>) v).entrySet()) {
                out.put(String.valueOf(render(e.getKey())), renderAny(e.getValue()));
            }
        }
        return out;
    }

    private static Object renderDescribed(DescribedType dt) {
        var code = dt.descriptorCode();
        var spec = Amqp10Schema.byCode(code);
        if (spec != null) {
            var out = new LinkedHashMap<String, Object>();
            out.put("type", spec.name);
            var fields = namedFields(spec, dt.getValue());
            if (fields != null && !fields.isEmpty()) {
                out.put("fields", fields);
            }
            return out;
        }
        var out = new LinkedHashMap<String, Object>();
        out.put("descriptor", dt.getDescriptor() instanceof AmqpSymbol
                ? ((AmqpSymbol) dt.getDescriptor()).getValue()
                : "0x" + Long.toHexString(code));
        out.put("value", renderAny(dt.getValue()));
        return out;
    }

    private static LinkedHashMap<String, Object> wrap(String type, Object value) {
        var out = new LinkedHashMap<String, Object>();
        out.put("type", type);
        out.put("value", value);
        return out;
    }

    /** The bytes from {@code from} as base64 (undecodable-section fallback). */
    private static String base64(byte[] frame, int from) {
        var rest = new byte[frame.length - from];
        System.arraycopy(frame, from, rest, 0, rest.length);
        return java.util.Base64.getEncoder().encodeToString(rest);
    }

    /** UTF-8 preview of a payload when it is printable text, else {@code null}. */
    static String printable(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        var s = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        for (int i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (c == 0xFFFD || (c < 0x20 && c != '\t' && c != '\n' && c != '\r')) {
                return null;
            }
        }
        return s;
    }
}
