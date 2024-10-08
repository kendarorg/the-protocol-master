package org.kendar.mqtt.utils;

public class VarBValue {
    private final long value;
    private final int length;

    public VarBValue(long value, int length) {
        this.value = value;
        this.length = length;
    }

    public long getValue() {
        return value;
    }

    public int getLength() {
        return length;
    }
}
