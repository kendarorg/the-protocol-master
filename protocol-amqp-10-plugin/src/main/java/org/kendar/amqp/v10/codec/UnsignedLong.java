package org.kendar.amqp.v10.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * AMQP 1.0 {@code ulong} (0..2^64-1). Carried in a long with unsigned semantics;
 * serialized as an unsigned decimal string for JSON fidelity.
 */
public final class UnsignedLong {
    private final long value; // raw 64 bits, interpreted unsigned

    @JsonCreator
    public UnsignedLong(String value) {
        this.value = Long.parseUnsignedLong(value);
    }

    public UnsignedLong(long rawBits) {
        this.value = rawBits;
    }

    public static UnsignedLong of(long v) {
        return new UnsignedLong(v);
    }

    public long getRawBits() {
        return value;
    }

    @JsonValue
    public String asString() {
        return Long.toUnsignedString(value);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof UnsignedLong) && ((UnsignedLong) o).value == value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return "ulong:" + asString();
    }
}
