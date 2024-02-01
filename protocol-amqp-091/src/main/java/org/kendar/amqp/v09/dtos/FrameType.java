package org.kendar.amqp.v09.dtos;

import java.util.HashMap;
import java.util.Map;

public enum FrameType {

    BODY(3),
    HEADER(2),
    HEARTHBIT(4),
    METHOD(1);
    private static final Map<Integer, FrameType> BY_INT = new HashMap<>();

    static {
        for (FrameType e : values()) {
            BY_INT.put(e.value, e);
        }
    }

    private final int value;

    FrameType(int value) {

        this.value = value;
    }

    public static FrameType of(int value) {
        return BY_INT.get(value);
    }

    public int getValue() {
        return value;
    }

    public byte asByte() {
        return (byte)value;
    }
}
