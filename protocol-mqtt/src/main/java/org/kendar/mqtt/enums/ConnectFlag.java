package org.kendar.mqtt.enums;

import java.util.HashMap;
import java.util.Map;

public enum ConnectFlag {
    USERNAME(0x80),
    PASSWORD(0x40),
    WILLRETAIN(0x20),
    WILLQOSTWO(0x10),
    WILLQOSONE(0x08),
    WILLFLAG(0x04),
    CLEANSESSION(0x02);

    private static final Map<Integer, ConnectFlag> BY_INT = new HashMap<>();

    static {
        for (ConnectFlag e : values()) {
            BY_INT.put((int) e.value, e);
        }
    }

    private final byte value;

    ConnectFlag(int value) {

        this.value = (byte) value;
    }

    public static boolean isFlagSet(int source, int flag) {
        return (source & flag) == flag;
    }

    public static boolean isFlagSet(int source, ConnectFlag flag) {
        return isFlagSet(source, flag.getValue());
    }

    public static int setFlag(int source, int flag) {
        return source |= flag;
    }

    public static int unsetFlag(int source, int flag) {
        return source & ~flag;
    }

    public static ConnectFlag of(int value) {
        return BY_INT.get(value);
    }

    public int getValue() {
        return value;
    }

    public byte asByte() {
        return value;
    }
}
