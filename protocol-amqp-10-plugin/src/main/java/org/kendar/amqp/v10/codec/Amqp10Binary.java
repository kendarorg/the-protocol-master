package org.kendar.amqp.v10.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Base64;

/** AMQP 1.0 {@code binary}. Serialized as base64 for JSON fidelity. */
public final class Amqp10Binary {
    private final byte[] value;

    public Amqp10Binary(byte[] value) {
        this.value = value == null ? new byte[0] : value;
    }

    @JsonCreator
    public static Amqp10Binary fromBase64(String b64) {
        return new Amqp10Binary(Base64.getDecoder().decode(b64));
    }

    public byte[] getValue() {
        return value;
    }

    @JsonValue
    public String toBase64() {
        return Base64.getEncoder().encodeToString(value);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Amqp10Binary) && Arrays.equals(value, ((Amqp10Binary) o).value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "binary[" + value.length + "]";
    }
}
