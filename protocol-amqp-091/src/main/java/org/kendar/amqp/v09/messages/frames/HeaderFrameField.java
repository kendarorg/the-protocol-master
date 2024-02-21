package org.kendar.amqp.v09.messages.frames;

import java.util.HashMap;
import java.util.Map;

public enum HeaderFrameField {
    FLAG_CONTENT_TYPE(0x8000),
    FLAG_CONTENT_ENCODING(0x4000),
    FLAG_HEADERS(0x2000),
    FLAG_DELIVERY_MODE(0x1000),
    FLAG_PRIORITY(0x0800),
    FLAG_CORRELATION_ID(0x0400),
    FLAG_REPLY_TO(0x0200),
    FLAG_EXPIRATION(0x0100),
    FLAG_MESSAGE_ID(0x0080),
    FLAG_TIMESTAMP(0x0040),
    FLAG_TYPE(0x0020),
    FLAG_USER_ID(0x0010),
    FLAG_APP_ID(0x0008),
    FLAG_RESERVED1(0x0004),
    FLAG_INVALID(0x0003);
    private static final Map<Integer, HeaderFrameField> BY_INT = new HashMap<>();

    static {
        for (HeaderFrameField e : values()) {
            BY_INT.put(e.value, e);
        }
    }

    private final int value;

    HeaderFrameField(int value) {

        this.value = value;
    }

    public static boolean isFlagSet(int source, int flag) {
        return (source & flag) == flag;
    }

    public static boolean isFlagSet(int source, HeaderFrameField flag) {
        return isFlagSet(source, flag.getValue());
    }

    public static int setFlag(int source, int flag) {
        return source |= flag;
    }

    public static int unsetFlag(int source, int flag) {
        return source & ~flag;
    }

    public static HeaderFrameField of(int value) {
        return BY_INT.get(value);
    }

    public int getValue() {
        return value;
    }

    public short asShort() {
        return (short) value;
    }
}
