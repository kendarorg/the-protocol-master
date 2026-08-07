package org.kendar.mssql.buffers;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;

import java.nio.charset.StandardCharsets;

/**
 * TDS oriented buffer: the 8 bytes packet header is big-endian but all the
 * payload fields are little-endian with UCS-2/UTF-16LE strings
 */
public class MssqlBBuffer extends BBuffer {

    public MssqlBBuffer() {
        this(BBufferEndianness.BE);
    }

    public MssqlBBuffer(BBufferEndianness endianness) {
        super(endianness);
    }

    public int readUShortLE() {
        int i = get() & 0xff;
        i |= (get() & 0xff) << 8;
        return i;
    }

    public void writeUShortLE(int i) {
        write((byte) (i & 0xff));
        write((byte) ((i >>> 8) & 0xff));
    }

    public long readUIntLE() {
        long i = get() & 0xffL;
        i |= (get() & 0xffL) << 8;
        i |= (get() & 0xffL) << 16;
        i |= (get() & 0xffL) << 24;
        return i;
    }

    public void writeUIntLE(long l) {
        write((byte) (l & 0xff));
        write((byte) ((l >>> 8) & 0xff));
        write((byte) ((l >>> 16) & 0xff));
        write((byte) ((l >>> 24) & 0xff));
    }

    public long readULongLE() {
        long result = 0;
        for (var i = 0; i < 8; i++) {
            result |= (get() & 0xffL) << (8 * i);
        }
        return result;
    }

    public void writeULongLE(long l) {
        for (var i = 0; i < 8; i++) {
            write((byte) ((l >>> (8 * i)) & 0xff));
        }
    }

    /**
     * B_VARCHAR: 1 byte char count followed by UTF-16LE chars
     */
    public String readBVarchar() {
        var chars = get() & 0xff;
        return new String(getBytes(chars * 2), StandardCharsets.UTF_16LE);
    }

    public void writeBVarchar(String value) {
        if (value == null) value = "";
        var data = value.getBytes(StandardCharsets.UTF_16LE);
        write((byte) (data.length / 2));
        write(data);
    }

    /**
     * US_VARCHAR: 2 bytes char count followed by UTF-16LE chars
     */
    public String readUsVarchar() {
        var chars = readUShortLE();
        return new String(getBytes(chars * 2), StandardCharsets.UTF_16LE);
    }

    public void writeUsVarchar(String value) {
        if (value == null) value = "";
        var data = value.getBytes(StandardCharsets.UTF_16LE);
        writeUShortLE(data.length / 2);
        write(data);
    }

    public void writeCollation(byte[] collation) {
        write(collation);
    }
}
