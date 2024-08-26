package org.kendar.mqtt.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum MqttFixedHeader {
    RESERVED(0x00),
    CONNECT(0x10),
    CONNACK(0x20),
    PUBLISH(0x30),
    //                        00110101
//    PUBLISH_RETAIN(0x31),   00110001
//    PUBLISH_QOS(0x36),      00110110
//    PUBLISH_DUP(0x38),      00111000
    //
    PUBACK(0x40),
    PUBREC(0x50),
    PUBREL(0x60),
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
    private static final List<Byte> bytes = new ArrayList<>();

    static {
        for (MqttFixedHeader e : values()) {
            BY_INT.put((int) e.value, e);
            bytes.add((byte) e.value);
        }

    }

    private final byte value;

    MqttFixedHeader(int value) {

        this.value = (byte) value;
    }


    public static MqttFixedHeader of(int value) {
        for (int i = bytes.size() - 1; i >= 0; i--) {
            var by = bytes.get(i);
            if (by != 0 && (by & value) == by) {
                return BY_INT.get((int) by);
            }
        }
        throw new RuntimeException("MISSING MESSAGE TYPE " + value);
    }

    public int getValue() {
        return value;
    }

    public byte asByte() {
        return value;
    }
}