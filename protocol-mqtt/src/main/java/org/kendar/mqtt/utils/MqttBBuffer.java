package org.kendar.mqtt.utils;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class MqttBBuffer extends BBuffer {

    public static final int VARIABLE_BYTE_INT_MAX = 268_435_455;

    public MqttBBuffer(BBufferEndianness endianness) {
        super(endianness);
    }

    public int writeVarBInteger(long number) {
        if (this.position == -1) {
            this.position = 0;
        }
        var writtenLen = writeVarBInteger(number, this.position);
        this.position += writtenLen;
        return writtenLen;
    }

    public int writeVarBInteger(long number, int offset) {

        int numBytes = 0;
        long no = number;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Encode the remaining length fields in the four bytes
        do {
            byte digit = (byte) (no % 128);
            no = no / 128;
            if (no > 0) {
                digit |= 0x80;
            }
            baos.write(digit);
            numBytes++;
        } while ((no > 0) && (numBytes < 4));
        var toWrite = baos.toByteArray();
        write(toWrite, offset);
        return toWrite.length;
    }

    public VarBValue readVarBInteger() {
        if (this.position == -1) {
            this.position = 0;
        }

        byte digit;
        int value = 0;
        int multiplier = 1;
        int count = 0;

        do {
            digit = this.bytes[this.position + count];
            count++;
            value += ((digit & 0x7F) * multiplier);
            multiplier *= 128;
        } while ((digit & 0x80) != 0);

        if (value < 0 || value > VARIABLE_BYTE_INT_MAX) {
            throw new RuntimeException("This property must be a number between 0 and " + VARIABLE_BYTE_INT_MAX
                    + ". Read value was: " + value);
        }

        this.position += count;
        return new VarBValue(value, count);
    }

    public VarBValue readVarBInteger(int offset) {
        byte digit;
        int value = 0;
        int multiplier = 1;
        int count = 0;

        do {
            digit = this.bytes[offset + count];
            count++;
            value += ((digit & 0x7F) * multiplier);
            multiplier *= 128;
        } while ((digit & 0x80) != 0);

        if (value < 0 || value > VARIABLE_BYTE_INT_MAX) {
            throw new RuntimeException("This property must be a number between 0 and " + VARIABLE_BYTE_INT_MAX
                    + ". Read value was: " + value);
        }

        return new VarBValue(value, count);
    }

    public int writeUtf8String(int offset, String value) {
        var data = value.getBytes(StandardCharsets.UTF_8);
        writeShort((short) data.length, offset);
        write(data, offset + 2);
        return data.length + offset + 2;
    }

    public void writeUtf8String(String value) {
        var data = value.getBytes(StandardCharsets.UTF_8);
        writeShort((short) data.length);
        write(data);
    }

    public String readUtf8String(int offset) {
        var length = getShort(offset);
        var data = getBytes(length, offset + 2);
        return new String(data);
    }

    public String readUtf8String() {
        var length = getShort();
        var data = getBytes(length);
        return new String(data);
    }
}
