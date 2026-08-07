package org.kendar.amqp.v10.codec;

import java.util.Objects;

/**
 * AMQP 1.0 described type: a descriptor (a {@link UnsignedLong} ulong code or an
 * {@link AmqpSymbol}) paired with a value (usually a {@code List} of fields for a
 * performative/section, but may be any type).
 */
public final class DescribedType {
    private Object descriptor;
    private Object value;

    public DescribedType() {
    }

    public DescribedType(Object descriptor, Object value) {
        this.descriptor = descriptor;
        this.value = value;
    }

    public Object getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(Object descriptor) {
        this.descriptor = descriptor;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /** Returns the descriptor as a ulong code, or -1 if it is not a ulong. */
    public long descriptorCode() {
        if (descriptor instanceof UnsignedLong) {
            return ((UnsignedLong) descriptor).getRawBits();
        }
        if (descriptor instanceof Number) {
            return ((Number) descriptor).longValue();
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DescribedType)) {
            return false;
        }
        var d = (DescribedType) o;
        return Objects.equals(descriptor, d.descriptor) && Objects.equals(value, d.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor, value);
    }

    @Override
    public String toString() {
        return "described(" + descriptor + ")=" + value;
    }
}
