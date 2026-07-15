package org.kendar.amqp.v10.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** AMQP 1.0 {@code ushort} (0..65535), carried in an int. */
public final class UnsignedShort {
    private final int value;

    @JsonCreator
    public UnsignedShort(int value) {
        this.value = value & 0xFFFF;
    }

    public static UnsignedShort of(int v) {
        return new UnsignedShort(v);
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof UnsignedShort) && ((UnsignedShort) o).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "ushort:" + value;
    }
}
