package org.kendar.mqtt.enums;

import java.util.HashMap;
import java.util.Map;

public enum MqttFixedHeader {
    RESERVED(0x00),
    CONNECT(0x10),
    CONNACK(0x20),
    PUBLISH(0x30),
    PUBLISH_RETAIN(0x31),
    PUBLISH_QOS(0x36),
    PUBLISH_DUP(0x38),
    PUBACK(0x40),
    PUBREC(0x50),
    PUBREL(0x62),
    PUBCOMP(0x70),
    SUBSCRIBE(0x82),
    SUBACK(0x90),
    UNSUBSCRIBE(0xA2),
    UNSUBACK(0xB0),
    PINGREQ(0xC0),
    PINGRESP(0xD0),
    DISCONNECT(0xE0),
    AUTH(0xF0);
    private static final Map<Integer, MqttFixedHeader> BY_INT = new HashMap<>();

    static {
        for (MqttFixedHeader e : values()) {
            BY_INT.put((int)e.value, e);
        }
    }

    private final byte value;

    MqttFixedHeader(int value) {

        this.value = (byte)value;
    }

    public static boolean isFlagSet(int source, int flag) {
        return (source & flag) == flag;
    }

    public static boolean isFlagSet(int source, MqttFixedHeader flag) {
        return isFlagSet(source, flag.getValue());
    }

    public static int setFlag(int source, int flag) {
        return source |= flag;
    }

    public static int unsetFlag(int source, int flag) {
        return source & ~flag;
    }

    public static MqttFixedHeader of(int value) {
        return BY_INT.get(value);
    }

    public int getValue() {
        return value;
    }

    public byte asByte() {
        return value;
    }
}