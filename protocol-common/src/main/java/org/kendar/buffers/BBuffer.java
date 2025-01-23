package org.kendar.buffers;

import java.nio.charset.StandardCharsets;

/**
 * Buffer with utility methods to read/write data in BE or LE
 */
@SuppressWarnings("DataFlowIssue")
public class BBuffer {

    protected final BBufferEndianness endianness;
    protected byte[] bytes = new byte[0];
    protected int position = -1;

    public BBuffer() {
        this(BBufferEndianness.BE);
    }

    public BBuffer(BBufferEndianness endianness) {
        this.endianness = endianness;
    }

    public static String toHexByteArray(byte[] byteArray) {
        StringBuilder hex = new StringBuilder();

        // Iterating through each byte in the array
        var endof = 0;
        for (byte i : byteArray) {
            hex.append("0x").append(String.format("%02X", i)).append(" ");
            if (endof == 16) {
                hex.append("\n");
                endof = 0;
            } else {
                endof++;
            }
        }

        return hex.toString();
    }

    public static float byte2Float(byte[] inData, boolean byteSwap) {
        int j = 0, upper, lower;
        if (!byteSwap) {
            //for (int i = 0; i < length; i++) {
            //j = i * 8;
            upper = (((inData[j] & 0xff) << 24)
                    + ((inData[j + 1] & 0xff) << 16)
                    + ((inData[j + 2] & 0xff) << 8)
                    + ((inData[j + 3] & 0xff) << 0));
            return Float.intBitsToFloat(upper);
        } else {
            lower = (((inData[j + 3] & 0xff) << 24)
                    + ((inData[j + 2] & 0xff) << 16)
                    + ((inData[j + 1] & 0xff) << 8)
                    + ((inData[j] & 0xff) << 0));
            return Float.intBitsToFloat(lower);
        }
        //}
    }

    public static double byte2Double(byte[] inData, boolean byteSwap) {
        int j = 0, upper, lower;
        if (!byteSwap) {
            //for (int i = 0; i < length; i++) {
            //j = i * 8;
            upper = (((inData[j] & 0xff) << 24)
                    + ((inData[j + 1] & 0xff) << 16)
                    + ((inData[j + 2] & 0xff) << 8) + ((inData[j + 3] & 0xff) << 0));
            lower = (((inData[j + 4] & 0xff) << 24)
                    + ((inData[j + 5] & 0xff) << 16)
                    + ((inData[j + 6] & 0xff) << 8) + ((inData[j + 7] & 0xff) << 0));
        } else {
            //for (int i = 0; i < length; i++) {
            upper = (((inData[j + 7] & 0xff) << 24)
                    + ((inData[j + 6] & 0xff) << 16)
                    + ((inData[j + 5] & 0xff) << 8) + ((inData[j + 4] & 0xff) << 0));
            lower = (((inData[j + 3] & 0xff) << 24)
                    + ((inData[j + 2] & 0xff) << 16)
                    + ((inData[j + 1] & 0xff) << 8) + ((inData[j] & 0xff) << 0));
        }
        return Double.longBitsToDouble((((long) upper) << 32)
                + (lower & 0xffffffffL));
        //}
    }

    public static BBuffer of(byte... bytes) {
        var bb = new BBuffer();
        bb.write(bytes);
        return bb;
    }

    public BBufferEndianness getEndianness() {
        return endianness;
    }

    public boolean isBe() {
        return endianness == BBufferEndianness.BE;
    }

    public byte[] getAll() {
        return bytes;
    }

    public byte[] getRemaining() {
        if (position < 0) return bytes;
        var toCopy = new byte[bytes.length - position];
        System.arraycopy(bytes, position, toCopy, 0, bytes.length - position);
        return toCopy;
    }

    public byte[] toArray() {
        return bytes;
    }

    public void write(byte second, int offset) {
        write(new byte[]{second}, offset);
    }

    public void write(byte second) {
        if (this.position == -1) {
            this.position = 0;
        }
        write(new byte[]{second}, this.position);
        this.position++;
    }


    public void write(byte[] second) {
        if (this.position == -1) {
            this.position = 0;
        }
        write(second, this.position);
        this.position += second.length;
    }

    public void writePartial(byte[] second, int length) {
        if (this.position == -1) {
            this.position = 0;
        }
        writePartial(second, length, this.position);
        this.position += length;
    }

    public void writePartial(byte[] second, int length, int offset) {
        if (offset < 0) offset = 0;
        if ((offset + second.length) > bytes.length) {
            var missingLength = (offset + length) - bytes.length;

            var first = bytes;
            bytes = new byte[missingLength + bytes.length];
            System.arraycopy(first, 0, bytes, 0, first.length);
            System.arraycopy(second, 0, bytes, first.length, length);
        } else {
            System.arraycopy(second, 0, bytes, offset, length);
        }
    }

    public void write(byte[] second, int offset) {
        if (offset < 0) offset = 0;
        if ((offset + second.length) > bytes.length) {
            var missingLength = (offset + second.length) - bytes.length;

            var first = bytes;
            bytes = new byte[missingLength + bytes.length];
            System.arraycopy(first, 0, bytes, 0, first.length);
            System.arraycopy(second, 0, bytes, first.length, second.length);
        } else {
            System.arraycopy(second, 0, bytes, offset, second.length);
        }
    }

    public byte[] getBytes(int length) {
        if (this.position == -1) {
            this.position = 0;
        }
        var result = getBytes(this.position, length);
        this.position += (length);
        return result;
    }

    public byte get() {


        if (this.position == -1) {
            this.position = 0;
        }
        var res = get(this.position);
        this.position++;
        return res;
    }

    public byte get(int position) {
        return bytes[position];
    }

    public byte[] getBytes(int position, int length) {
        var dst = new byte[length];
        System.arraycopy(bytes, position, dst, 0, length);
        return dst;
    }

    public int size() {
        return bytes.length;
    }

    public void resetPosition() {
        this.setPosition(-1);
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int newPosition) {
        this.position = newPosition;
    }

    public void truncate() {
        truncate(this.position);
    }

    public void truncate(int position) {
        if (position < 0) position = 0;
        var remainingLength = bytes.length - position;
        if (remainingLength < 0) {
            bytes = new byte[0];
        } else {
            var src = bytes;
            if (remainingLength > 0) {
                var dst = new byte[remainingLength];
                System.arraycopy(src, position, dst, 0, remainingLength);
                bytes = dst;
            } else {
                bytes = new byte[0];
            }
        }
        this.position = -1;
    }

    public int getInt() {
        if (this.position == -1) {
            this.position = 0;
        }
        var result = getInt(this.position);
        this.position += 4;
        return result;
    }

    public int getInt(int position) {
        if (this.position == -1) {
            this.position = 0;
        }
        var intBytes = getBytes(position, 4);
        if (endianness == BBufferEndianness.LE) {
            intBytes = BBEndiannessConverter.swap4Bytes(intBytes);
        }
        int intValue = 0;
        for (byte b : intBytes) {
            intValue = (intValue << 8) + (b & 0xFF);
        }
        return intValue;

    }

    public short getShort() {
        if (this.position == -1) {
            this.position = 0;
        }
        var result = getShort(this.position);
        this.position += 2;
        return result;
    }

    public short getShort(int position) {
        var intBytes = getBytes(position, 2);
        if (endianness == BBufferEndianness.LE) {
            intBytes = BBEndiannessConverter.swap2Bytes(intBytes);
        }
        int intValue = 0;
        for (byte b : intBytes) {
            intValue = (intValue << 8) + (b & 0xFF);
        }
        return (short) intValue;

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
        var readBuffer = getBytes(position, 8);
        if (endianness == BBufferEndianness.LE) {
            readBuffer = BBEndiannessConverter.swap8Bytes(readBuffer);
        }

        return (((long) readBuffer[0] << 56) +
                ((long) (readBuffer[1] & 255) << 48) +
                ((long) (readBuffer[2] & 255) << 40) +
                ((long) (readBuffer[3] & 255) << 32) +
                ((long) (readBuffer[4] & 255) << 24) +
                ((readBuffer[5] & 255) << 16) +
                ((readBuffer[6] & 255) << 8) +
                ((readBuffer[7] & 255) << 0));
    }

    public boolean contains(byte[] toSearch, int offset) {
        if (bytes.length < (toSearch.length + offset)) return false;
        for (var i = 0; i < toSearch.length; i++) {
            if (toSearch[i] != bytes[offset + i]) {
                return false;
            }
        }
        return true;
    }


    public void writeShort(short value, int offset) {
        var data = new byte[]{
                (byte) (value >> 8),
                (byte) value};
        if (this.endianness == BBufferEndianness.LE) {
            data = BBEndiannessConverter.swap2Bytes(data);
        }
        write(data, offset);

    }

    public void writeShort(short value) {
        if (this.position == -1) {
            this.position = 0;
        }
        writeShort(value, this.position);
        this.position += 2;
    }

    public void writeLong(long v, int offset) {
        byte[] writeBuffer = new byte[Long.BYTES];
        writeBuffer[0] = (byte) (v >>> 56);
        writeBuffer[1] = (byte) (v >>> 48);
        writeBuffer[2] = (byte) (v >>> 40);
        writeBuffer[3] = (byte) (v >>> 32);
        writeBuffer[4] = (byte) (v >>> 24);
        writeBuffer[5] = (byte) (v >>> 16);
        writeBuffer[6] = (byte) (v >>> 8);
        writeBuffer[7] = (byte) (v >>> 0);
        if (this.endianness == BBufferEndianness.LE) {
            writeBuffer = BBEndiannessConverter.swap8Bytes(writeBuffer);
        }
        write(writeBuffer, offset);

    }

    public void writeLong(long value) {
        if (this.position == -1) {
            this.position = 0;
        }
        writeLong(value, this.position);
        this.position += 8;
    }

    public void reset() {
        this.position = -1;
        this.bytes = new byte[]{};
    }

    public String getString() {
        var index = findNextCharPosition(this.position, (byte) 0x00);
        if (index == -1) index = bytes.length;
        var result = new String(bytes, this.position, index - this.position, StandardCharsets.US_ASCII);
        this.position = index + 1;
        return result;
    }

    public String getString(int offset) {
        var index = findNextCharPosition(offset, (byte) 0x00);
        if (index == -1) index = bytes.length;
        return new String(bytes, offset, index - offset, StandardCharsets.US_ASCII);
    }

    public String getUtf8String() {
        var index = findNextCharPosition(this.position, (byte) 0x00);
        if (index == -1) index = bytes.length;
        var result = new String(bytes, this.position, index - this.position, StandardCharsets.UTF_8);
        this.position = index + 1;
        return result;
    }

    public String getUtf8String(int offset) {
        var index = findNextCharPosition(offset, (byte) 0x00);
        if (index == -1) index = bytes.length;
        return new String(bytes, offset, index - offset, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("SameParameterValue")
    private int findNextCharPosition(int offset, byte toFind) {
        for (var i = offset; i < bytes.length; i++) {
            if (bytes[i] == toFind) {
                return i;
            }
        }
        return -1;
    }

    public Double getDouble() {
        var data = getBytes(8);
        return byte2Double(data, this.endianness == BBufferEndianness.LE);
    }

    public Double getDouble(int position) {
        var data = getBytes(position, 8);
        return byte2Double(data, this.endianness == BBufferEndianness.LE);
    }

    public Float getFloat() {
        var data = getBytes(8);
        return byte2Float(data, this.endianness == BBufferEndianness.LE);
    }

    public Float getFloat(int position) {
        var data = getBytes(position, 4);
        return byte2Float(data, this.endianness == BBufferEndianness.LE);
    }

    public void append(BBuffer buffer) {
        write(buffer.bytes, bytes.length);
    }

    public void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeInt(int value) {
        if (this.position == -1) {
            this.position = 0;
        }
        writeInt(value, this.position);
        this.position += 4;
    }

    public void writeInt(int value, int offset) {
        var data = new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
        if (this.endianness == BBufferEndianness.LE) {
            data = BBEndiannessConverter.swap4Bytes(data);
        }
        write(data, offset);

    }


    public void writeUnsignedInt(int value) {
        if (this.position == -1) {
            this.position = 0;
        }
        writeUnsignedInt(value, this.position);
        this.position += 4;
    }

    public void writeUnsignedInt(int value, int offset) {
        var data = new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
        BBufferUtils.unSetBit(data, 0);
        if (this.endianness == BBufferEndianness.LE) {
            data = BBEndiannessConverter.swap4Bytes(data);
        }
        write(data, offset);

    }

    public int indexOf(byte[] needle) {
        // needle is null or empty
        if (needle == null || needle.length == 0)
            return 0;

        // haystack is null, or haystack's length is less than that of needle
        if (this.bytes == null || needle.length > this.bytes.length)
            return -1;

        // pre construct failure array for needle pattern
        int[] failure = new int[needle.length];
        int n = needle.length;
        failure[0] = -1;
        for (int j = 1; j < n; j++) {
            int i = failure[j - 1];
            while ((needle[j] != needle[i + 1]) && i >= 0)
                i = failure[i];
            if (needle[j] == needle[i + 1])
                failure[j] = i + 1;
            else
                failure[j] = -1;
        }

        // find match
        int i = 0, j = 0;
        int haystackLen = this.bytes.length;
        int needleLen = needle.length;
        while (i < haystackLen && j < needleLen) {
            if (this.bytes[i] == needle[j]) {
                i++;
                j++;
            } else if (j == 0)
                i++;
            else
                j = failure[j - 1] + 1;
        }
        return ((j == needleLen) ? (i - needleLen) : -1);
    }

    public String toHexStringUpToLength(int length) {
        var size = Math.min(size(), length);
        return toHexByteArray(getBytes(size));
    }

    public String toHexStringUpToLength(int pos, int length) {
        var size = Math.min(size(), length + pos);
        return toHexByteArray(getBytes(pos, size));
    }
}
