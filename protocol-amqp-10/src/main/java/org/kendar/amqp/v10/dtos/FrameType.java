package org.kendar.amqp.v10.dtos;

import java.util.HashMap;
import java.util.Map;

/**
 * AMQP 1.0 frame "type" byte carried in the fixed header (offset 5).
 * AMQP performative frames are type 0, SASL frames are type 1.
 */
public enum FrameType {

    AMQP(0),
    SASL(1);

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
        return (byte) value;
    }
}
