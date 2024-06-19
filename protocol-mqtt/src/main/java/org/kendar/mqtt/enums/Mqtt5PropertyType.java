package org.kendar.mqtt.enums;

import java.util.HashMap;
import java.util.Map;

public enum Mqtt5PropertyType {
    PAYLOAD_FORMAT_INDICATOR(0x01),
    MESSAGE_EXPIRY_INTERVAL(0x02),
    CONTENT_TYPE(0x03),
    RESPONSE_TOPIC(0x08),
    CORRELATION_DATA(0x09),
    SUBSCRIPTION_IDENTIFIER(0x0B),
    SESSION_EXPIRY_INTERVAL(0x11),
    ASSIGEND_CLIENT_IDENTIFIER(0x12),
    SERVER_KEEP_ALIVE(0x13),
    AUTHENTICATION_METHOD(0x15),
    AUTHENTICATION_DATA(0x17),
    REQUEST_PROBLEM_INFORMATION(0x11),
    WILL_DELAY_INTERVAL(0x18),
    REQUEST_RESPONSE_INFORMATION(0x19),
    RESPONSE_INFORMATION(0x1A),
    SERVER_REFERENCE(0x1C),
    REASON_STRING(0x1F),
    RECEIVE_MAXIMUM(0x21),
    TOPIC_ALIAS_MAXIMUM(0x22),
    TOPIC_ALIAS(0x23),
    MAXIMUM_QOS(0x24),
    RETAIN_AVAILABLE(0x25),
    USER_PROPERTY(0x26),
    MAXIMUM_PACKET_SIZE(0x27),
    WILDCARD_SUBSCRIPTION_AVAILABLE(0x28),
    SUBSCRIPTION_IDENTIFIER_AVAILABLE(0x29),
    SHARED_SUBSCRIPTION_AVAILABLE(0x11);

    private static final Map<Integer, Mqtt5PropertyType> BY_INT = new HashMap<>();

    static {
        for (Mqtt5PropertyType e : values()) {
            BY_INT.put((int)e.value, e);
        }
    }

    private final byte value;

    Mqtt5PropertyType(int value) {

        this.value = (byte)value;
    }

    public static boolean isFlagSet(int source, int flag) {
        return (source & flag) == flag;
    }

    public static boolean isFlagSet(int source, Mqtt5PropertyType flag) {
        return isFlagSet(source, flag.getValue());
    }

    public static int setFlag(int source, int flag) {
        return source |= flag;
    }

    public static int unsetFlag(int source, int flag) {
        return source & ~flag;
    }

    public static Mqtt5PropertyType of(int value) {
        return BY_INT.get(value);
    }

    public int getValue() {
        return value;
    }

    public byte asByte() {
        return value;
    }
}
