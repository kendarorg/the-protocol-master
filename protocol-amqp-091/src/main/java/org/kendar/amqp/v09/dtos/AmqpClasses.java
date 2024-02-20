package org.kendar.amqp.v09.dtos;

import java.util.HashMap;
import java.util.Map;

public enum AmqpClasses {

    CONNECTION(10),
    CHANNEL(20),
    EXCHANGE(40),
    QUEUE(50),
    BASIC(60),
    CONFIRM(85),
    TX(90);
    private static final Map<Integer, AmqpClasses> BY_INT = new HashMap<>();

    static {
        for (AmqpClasses e : values()) {
            BY_INT.put(e.value, e);
        }
    }

    private final int value;

    AmqpClasses(int value) {

        this.value = value;
    }

    public static AmqpClasses of(int value) {
        return BY_INT.get(value);
    }

    public int getValue() {
        return value;
    }

    public short asShort() {
        return (short) value;
    }
}
