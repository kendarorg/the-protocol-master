package org.kendar.amqp.v10.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** AMQP 1.0 {@code char} (a 32-bit UTF-32 code point). */
public final class AmqpChar {
    private final int codePoint;

    @JsonCreator
    public AmqpChar(int codePoint) {
        this.codePoint = codePoint;
    }

    @JsonValue
    public int getCodePoint() {
        return codePoint;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof AmqpChar) && ((AmqpChar) o).codePoint == codePoint;
    }

    @Override
    public int hashCode() {
        return codePoint;
    }

    @Override
    public String toString() {
        return "char:" + new String(Character.toChars(codePoint));
    }
}
