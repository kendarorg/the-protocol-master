package org.kendar.amqp.v10.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** AMQP 1.0 {@code uint} (0..2^32-1), carried in a long. */
public final class UnsignedInt {
    private final long value;

    @JsonCreator
    public UnsignedInt(long value) {
        this.value = value & 0xFFFFFFFFL;
    }

    public static UnsignedInt of(long v) {
        return new UnsignedInt(v);
    }

    @JsonValue
    public long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof UnsignedInt) && ((UnsignedInt) o).value == value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return "uint:" + value;
    }
}
