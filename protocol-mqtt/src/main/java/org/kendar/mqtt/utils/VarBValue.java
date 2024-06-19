package org.kendar.mqtt.utils;

public class VarBValue {
    public VarBValue(long value, int length) {
        this.value = value;
        this.length = length;
    }

    private long value;
    private int length;

    public long getValue() {
        return value;
    }

    public int getLength() {
        return length;
    }
}
