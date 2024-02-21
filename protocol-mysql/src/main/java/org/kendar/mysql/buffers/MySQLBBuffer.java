package org.kendar.mysql.buffers;

import org.kendar.buffers.BBEndiannessConverter;
import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;

public class MySQLBBuffer extends BBuffer {

    public static final long NULL_LENGTH = -1;
    private static final byte[] EMPTY_BYTES = new byte[0];

    public MySQLBBuffer() {
        this(BBufferEndianness.BE);
    }

    public MySQLBBuffer(BBufferEndianness endianness) {
        super(endianness);
    }


    public void writeUB4(long l) {
        write((byte) (l & 0xff));
        write((byte) (l >>> 8));
        write((byte) (l >>> 16));
        write((byte) (l >>> 24));
    }

    public void writeUB2(int i) {
        write((byte) (i & 0xff));
        write((byte) (i >>> 8));
    }

    public void writeUB3(int i) {
        write((byte) (i & 0xff));
        write((byte) (i >>> 8));
        write((byte) (i >>> 16));
    }

    public int readUB3() {
        int i = get() & 0xff;
        i |= (get() & 0xff) << 8;
        i |= (get() & 0xff) << 16;
        return i;
    }

    public int readUB4() {
        int i = get() & 0xff;
        i |= (get() & 0xff) << 8;
        i |= (get() & 0xff) << 16;
        i |= (get() & 0xff) << 24;
        return i;
    }

    public byte[] readBytesWithLength() {
        int length = (int) readLength();
        if (length == NULL_LENGTH) {
            return null;
        }
        if (length <= 0) {
            return EMPTY_BYTES;
        }
        return getBytes(length);
    }

    public int readUB2() {
        int i = get() & 0xff;
        i |= (get() & 0xff) << 8;
        return i;
    }


    public long readLength() {
        int length = get() & 0xff;
        switch (length) {
            case 251:
                return NULL_LENGTH;
            case 252:
                return readUB2();
            case 253:
                return readUB3();
            case 254:
                return getLong();
            default:
                return length;
        }
    }

    public void writeWithLength(byte[] src) {
        long length = src.length;
        writeLength(length);
        write(src);
    }

    public void writeLength(long l) {
        if (l < 0xfb) {
            write((byte) (l & 0xff));
        } else if (l < 0x10000) {
            write((byte) 0xfc);
            writeUB2((int) l);
        } else if (l < 0x1000000) {
            write((byte) 0xfd);
            writeUB3((int) l);
        } else {
            write((byte) 0xfe);
            writeLong(l);
        }
    }

    public void writeDoubleLe(double v) {
        var value = Double.doubleToLongBits(v);
        byte[] data = new byte[Long.BYTES];
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= Byte.SIZE;
        }
        if (this.isBe()) {
            data = BBEndiannessConverter.swap8Bytes(data, 0);
        }
        write(data);
    }

    public void writeFloatLe(float v) {
        var value = Float.floatToIntBits(v);
        var data = new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
        if (this.isBe()) {
            data = BBEndiannessConverter.swap4Bytes(data, 0);
        }
        write(data);
    }

    public Double getDoubleLe() {
        var data = getBytes(8);
        return byte2Double(data, isBe());
    }

    public Float getFloatLe() {
        var data = getBytes(4);
        return byte2Float(data, isBe());
    }

    public void writeLong(long value, int offset) {
        byte[] data = new byte[Long.BYTES];
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= Byte.SIZE;
        }
        if (this.endianness == BBufferEndianness.LE) {
            data = BBEndiannessConverter.swap8Bytes(data, 0);
        }
        write(data, offset);

    }

    public void writeLong(long value) {
        if (this.position == -1) {
            this.position = 0;
        }
        writeLong(value, this.position);
        this.position += 8;
    }


    public long getLong() {
        if (this.position == -1) {
            this.position = 0;
        }
        var result = getLong(this.position);
        this.position += 8;
        return result;
    }

    public long getLong(int position) {
        var intBytes = getBytes(position, 8);
        if (endianness == BBufferEndianness.LE) {
            intBytes = BBEndiannessConverter.swap8Bytes(intBytes, 0);
        }
        long value = 0;
        for (int i = 0; i < intBytes.length; i++) {
            value += ((long) intBytes[i] & 0xffL) << (8 * i);
        }
        return value;
    }
}
