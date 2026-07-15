package org.kendar.amqp.v10.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** AMQP 1.0 {@code timestamp} (ms since the Unix epoch), distinct from a plain long. */
public final class AmqpTimestamp {
    private final long millis;

    @JsonCreator
    public AmqpTimestamp(long millis) {
        this.millis = millis;
    }

    @JsonValue
    public long getMillis() {
        return millis;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof AmqpTimestamp) && ((AmqpTimestamp) o).millis == millis;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(millis);
    }

    @Override
    public String toString() {
        return "ts:" + millis;
    }
}
