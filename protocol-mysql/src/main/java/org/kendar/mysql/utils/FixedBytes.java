package org.kendar.mysql.utils;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;

public class FixedBytes {

    private static BBuffer createBuffer() {
        return new BBuffer(BBufferEndianness.LE);
    }

    public static byte[] writeInt(int toWrite, int length) {
        var bb = createBuffer();
        bb.writeInt(toWrite);
        bb.setPosition(0);
        var other = bb.getBytes(4);
        var result = new byte[length];
        for (var i = 0; i < 4; i++) {
            if (i < length) {
                result[i] = other[i];
            } else {
                if (other[i] != 0x00) {
                    throw new RuntimeException("WRONG APPROXIMATION");
                }
            }
        }
        return result;
    }

    public static byte[] writeLong(long toWrite, int length) {
        var bb = createBuffer();
        bb.writeLong(toWrite);
        bb.setPosition(0);
        var other = bb.getBytes(8);
        var result = new byte[length];
        for (var i = 0; i < 8; i++) {
            if (i < length) {
                result[i] = other[i];
            } else {
                if (other[i] != 0x00) {
                    throw new RuntimeException("WRONG APPROXIMATION");
                }
            }
        }
        return result;
    }

    public static byte[] writeIntLe(int toWrite, int length) {
        var bb = createBuffer();
        bb.writeInt(toWrite);
        bb.setPosition(0);
        var other = bb.getBytes(4);
        var lastZero = 3;
        for (var i = 3; i >= 0; i--) {
            if (other[i] != 0) {
                lastZero = i + 1;
                break;
            }
        }
        var newArray = new byte[lastZero + 1];
        if (lastZero == 1) {
            newArray[0] = (byte) 0xFA;
        } else if (lastZero == 2) {
            newArray[0] = (byte) 0xFC;
        } else if (lastZero == 3) {
            newArray[0] = (byte) 0xFD;
        }
        System.arraycopy(other, 0, newArray, 1, lastZero);
        return newArray;
    }
}
