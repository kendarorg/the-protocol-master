package org.kendar.amqp.v10.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** AMQP 1.0 {@code ubyte} (0..255), carried in a short. */
public final class UnsignedByte {
    private final short value;

    @JsonCreator
    public UnsignedByte(short value) {
        this.value = (short) (value & 0xFF);
    }

    public static UnsignedByte of(int v) {
        return new UnsignedByte((short) v);
    }

    @JsonValue
    public short getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof UnsignedByte) && ((UnsignedByte) o).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "ubyte:" + value;
    }
}
