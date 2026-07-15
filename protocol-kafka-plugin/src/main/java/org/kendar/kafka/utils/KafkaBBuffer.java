package org.kendar.kafka.utils;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Kafka wire-format codec primitives on top of {@link BBuffer}. Kafka is always
 * big-endian. Adds the KIP-482 varint / compact families used by the flexible
 * (tagged-field) encodings, plus classic and compact strings/arrays.
 * <p>
 * Tagged fields and record batches are always treated as <b>opaque</b> byte
 * blobs (read raw, re-written verbatim) — we never interpret or recompute them,
 * so CRC32C is never recomputed (protocol-kafka.md §3.3 / §6.1).
 */
public class KafkaBBuffer extends BBuffer {

    public KafkaBBuffer() {
        super(BBufferEndianness.BE);
    }

    public KafkaBBuffer(byte[] bytes) {
        super(BBufferEndianness.BE);
        write(bytes);
        setPosition(0);
    }

    // ---- KIP-482 unsigned varint (LEB128) ----

    public int readUnsignedVarint() {
        int value = 0;
        int i = 0;
        int b;
        while (((b = get() & 0xFF) & 0x80) != 0) {
            value |= (b & 0x7F) << i;
            i += 7;
            if (i > 28) {
                throw new IllegalArgumentException("Kafka unsigned varint too long");
            }
        }
        value |= b << i;
        return value;
    }

    public void writeUnsignedVarint(int value) {
        while ((value & 0xFFFFFF80) != 0) {
            write((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        write((byte) (value & 0x7F));
    }

    // ---- zigzag signed varint / varlong (used by record batches, M2+) ----

    public int readVarint() {
        int raw = readUnsignedVarint();
        return (raw >>> 1) ^ -(raw & 1);
    }

    public long readVarlong() {
        long value = 0;
        int i = 0;
        long b;
        while (((b = get() & 0xFF) & 0x80) != 0) {
            value |= (b & 0x7F) << i;
            i += 7;
            if (i > 63) {
                throw new IllegalArgumentException("Kafka varlong too long");
            }
        }
        value |= b << i;
        return (value >>> 1) ^ -(value & 1);
    }

    public void writeVarint(int value) {
        writeUnsignedVarint((value << 1) ^ (value >> 31));
    }

    public void writeVarlong(long value) {
        long zigzag = (value << 1) ^ (value >> 63);
        while ((zigzag & 0xFFFFFFFFFFFFFF80L) != 0) {
            write((byte) ((zigzag & 0x7F) | 0x80));
            zigzag >>>= 7;
        }
        write((byte) (zigzag & 0x7F));
    }

    // ---- compact bytes (unsigned-varint length+1; 0 = null) ----

    public void writeCompactBytes(byte[] value) {
        if (value == null) {
            writeUnsignedVarint(0);
            return;
        }
        writeUnsignedVarint(value.length + 1);
        write(value);
    }

    public byte[] readCompactBytes() {
        int len = readUnsignedVarint();
        if (len == 0) {
            return null;
        }
        return getBytes(len - 1);
    }

    // ---- classic (non-flexible) strings ----

    /** int16 length prefix; -1 means null. */
    public String readString() {
        short len = getShort();
        if (len < 0) {
            return null;
        }
        return new String(getBytes(len), StandardCharsets.UTF_8);
    }

    public void writeString(String value) {
        if (value == null) {
            writeShort((short) -1);
            return;
        }
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        writeShort((short) bytes.length);
        write(bytes);
    }

    // ---- compact (flexible) strings ----

    /** unsigned-varint (length+1); 0 means null. */
    public String readCompactString() {
        int len = readUnsignedVarint();
        if (len == 0) {
            return null;
        }
        return new String(getBytes(len - 1), StandardCharsets.UTF_8);
    }

    public void writeCompactString(String value) {
        if (value == null) {
            writeUnsignedVarint(0);
            return;
        }
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        writeUnsignedVarint(bytes.length + 1);
        write(bytes);
    }

    /** Reads a string, compact or classic depending on {@code flexible}. */
    public String readString(boolean flexible) {
        return flexible ? readCompactString() : readString();
    }

    public void writeString(String value, boolean flexible) {
        if (flexible) {
            writeCompactString(value);
        } else {
            writeString(value);
        }
    }

    // ---- arrays ----

    /** Element count: int32 (classic, -1 = null) or unsigned-varint minus 1 (compact, 0 = null). */
    public int readArrayCount(boolean flexible) {
        if (flexible) {
            int n = readUnsignedVarint();
            return n - 1;
        }
        return getInt();
    }

    public void writeArrayCount(int count, boolean flexible) {
        if (flexible) {
            writeUnsignedVarint(count < 0 ? 0 : count + 1);
        } else {
            writeInt(count);
        }
    }

    // ---- tagged fields (opaque) ----

    /**
     * Reads a tagged-field section verbatim and returns its raw encoding so it can
     * be written back unchanged. Never interprets the contents.
     */
    public byte[] readTaggedFieldsRaw() {
        int start = getPosition();
        int count = readUnsignedVarint();
        for (int i = 0; i < count; i++) {
            readUnsignedVarint();        // tag
            int size = readUnsignedVarint();
            getBytes(size);              // opaque value
        }
        int end = getPosition();
        setPosition(start);
        var raw = getBytes(end - start);
        return raw;
    }

    public UUID readUuid() {
        long hi = getLong();
        long lo = getLong();
        return new UUID(hi, lo);
    }

    public void writeUuid(UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    /** Remaining bytes from the current position to the end (does not advance). */
    public byte[] peekRemaining() {
        int pos = getPosition();
        var rest = getBytes(size() - pos);
        setPosition(pos);
        return rest;
    }
}
