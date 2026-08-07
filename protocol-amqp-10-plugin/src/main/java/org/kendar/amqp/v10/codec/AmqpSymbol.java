package org.kendar.amqp.v10.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/** AMQP 1.0 {@code symbol} (ASCII). Distinct Java type so a symbol round-trips as a symbol. */
public final class AmqpSymbol {
    private final String value;

    @JsonCreator
    public AmqpSymbol(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof AmqpSymbol) && Objects.equals(value, ((AmqpSymbol) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return "sym:" + value;
    }
}
