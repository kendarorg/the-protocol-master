package org.kendar.amqp.v10.codec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared field schema for AMQP 1.0 performatives, SASL frames, message sections
 * and nested described types: descriptor code, spec field names and wire types.
 * Used by {@link Amqp10FrameDescriber} (raw → readable JSON) and
 * {@link Amqp10FrameEncoder} (readable JSON → raw) so the two stay symmetric —
 * a field the describer renders as a plain number is a field whose wire type the
 * encoder can restore from this table.
 */
public final class Amqp10Schema {

    /** Wire type of a schema field. */
    public enum FieldType {
        UINT, ULONG, UBYTE, USHORT, BOOL, STR, SYM, BIN, TS,
        /** {@code multiple(symbol)}: single symbol or an array of symbols. */
        MULTI_SYM,
        /** Symbol-keyed map (annotations, filters, properties maps). */
        MAP_SYM,
        /** String-keyed map (application-properties). */
        MAP_STR,
        /** Map with untyped keys (attach unsettled). */
        MAP_ANY,
        /** Untyped: rendered/encoded with type wrappers for ambiguous scalars. */
        ANY,
        /** Nested described type ({@code {"type": name, "fields": {...}}}). */
        DESCRIBED
    }

    public static final class Spec {
        public final long code;
        public final String name;
        public final String[] names;
        public final FieldType[] types;

        Spec(long code, String name, String[] names, FieldType[] types) {
            this.code = code;
            this.name = name;
            this.names = names;
            this.types = types;
        }
    }

    private static final Map<String, Spec> BY_NAME = new LinkedHashMap<>();
    private static final Map<Long, Spec> BY_CODE = new LinkedHashMap<>();

    static {
        var u = FieldType.UINT;
        var ul = FieldType.ULONG;
        var ub = FieldType.UBYTE;
        var us = FieldType.USHORT;
        var b = FieldType.BOOL;
        var s = FieldType.STR;
        var sy = FieldType.SYM;
        var bin = FieldType.BIN;
        var ts = FieldType.TS;
        var ms = FieldType.MULTI_SYM;
        var mps = FieldType.MAP_SYM;
        var any = FieldType.ANY;
        var d = FieldType.DESCRIBED;

        spec(0x10, "open",
                n("container-id", "hostname", "max-frame-size", "channel-max", "idle-time-out",
                        "outgoing-locales", "incoming-locales", "offered-capabilities",
                        "desired-capabilities", "properties"),
                t(s, s, u, us, u, ms, ms, ms, ms, mps));
        spec(0x11, "begin",
                n("remote-channel", "next-outgoing-id", "incoming-window", "outgoing-window",
                        "handle-max", "offered-capabilities", "desired-capabilities", "properties"),
                t(us, u, u, u, u, ms, ms, mps));
        spec(0x12, "attach",
                n("name", "handle", "role", "snd-settle-mode", "rcv-settle-mode", "source", "target",
                        "unsettled", "incomplete-unsettled", "initial-delivery-count",
                        "max-message-size", "offered-capabilities", "desired-capabilities", "properties"),
                t(s, u, b, ub, ub, d, d, FieldType.MAP_ANY, b, u, ul, ms, ms, mps));
        spec(0x13, "flow",
                n("next-incoming-id", "incoming-window", "next-outgoing-id", "outgoing-window",
                        "handle", "delivery-count", "link-credit", "available", "drain", "echo",
                        "properties"),
                t(u, u, u, u, u, u, u, u, b, b, mps));
        spec(0x14, "transfer",
                n("handle", "delivery-id", "delivery-tag", "message-format", "settled", "more",
                        "rcv-settle-mode", "state", "resume", "aborted", "batchable"),
                t(u, u, bin, u, b, b, ub, d, b, b, b));
        spec(0x15, "disposition",
                n("role", "first", "last", "settled", "state", "batchable"),
                t(b, u, u, b, d, b));
        spec(0x16, "detach", n("handle", "closed", "error"), t(u, b, d));
        spec(0x17, "end", n("error"), t(d));
        spec(0x18, "close", n("error"), t(d));

        spec(0x40, "sasl-mechanisms", n("sasl-server-mechanisms"), t(ms));
        spec(0x41, "sasl-init", n("mechanism", "initial-response", "hostname"), t(sy, bin, s));
        spec(0x42, "sasl-challenge", n("challenge"), t(bin));
        spec(0x43, "sasl-response", n("response"), t(bin));
        spec(0x44, "sasl-outcome", n("code", "additional-data"), t(ub, bin));

        spec(0x70, "header",
                n("durable", "priority", "ttl", "first-acquirer", "delivery-count"),
                t(b, ub, u, b, u));
        spec(0x71, "delivery-annotations", n(), t());
        spec(0x72, "message-annotations", n(), t());
        spec(0x73, "properties",
                n("message-id", "user-id", "to", "subject", "reply-to", "correlation-id",
                        "content-type", "content-encoding", "absolute-expiry-time", "creation-time",
                        "group-id", "group-sequence", "reply-to-group-id"),
                t(any, bin, s, s, s, any, sy, sy, ts, ts, s, u, s));
        spec(0x74, "application-properties", n(), t());
        spec(0x75, "data", n(), t());
        spec(0x76, "amqp-sequence", n(), t());
        spec(0x77, "amqp-value", n(), t());
        spec(0x78, "footer", n(), t());

        spec(0x28, "source",
                n("address", "durable", "expiry-policy", "timeout", "dynamic",
                        "dynamic-node-properties", "distribution-mode", "filter", "default-outcome",
                        "outcomes", "capabilities"),
                t(s, u, sy, u, b, mps, sy, mps, d, ms, ms));
        spec(0x29, "target",
                n("address", "durable", "expiry-policy", "timeout", "dynamic",
                        "dynamic-node-properties", "capabilities"),
                t(s, u, sy, u, b, mps, ms));
        spec(0x1D, "error", n("condition", "description", "info"), t(sy, s, mps));
        spec(0x23, "received", n("section-number", "section-offset"), t(u, ul));
        spec(0x24, "accepted", n(), t());
        spec(0x25, "rejected", n("error"), t(d));
        spec(0x26, "released", n(), t());
        spec(0x27, "modified",
                n("delivery-failed", "undeliverable-here", "message-annotations"),
                t(b, b, mps));
    }

    private Amqp10Schema() {
    }

    private static String[] n(String... names) {
        return names;
    }

    private static FieldType[] t(FieldType... types) {
        return types;
    }

    private static void spec(long code, String name, String[] names, FieldType[] types) {
        var spec = new Spec(code, name, names, types);
        BY_NAME.put(name, spec);
        BY_CODE.put(code, spec);
    }

    public static Spec byName(String name) {
        return BY_NAME.get(name);
    }

    public static Spec byCode(long code) {
        return BY_CODE.get(code);
    }

    public static String nameOf(long code) {
        var spec = BY_CODE.get(code);
        return spec != null ? spec.name : null;
    }
}
