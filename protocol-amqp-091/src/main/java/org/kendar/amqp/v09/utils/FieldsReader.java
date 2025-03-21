package org.kendar.amqp.v09.utils;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.TPMProtocolException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@SuppressWarnings("DuplicateBranchesInSwitch")
public class FieldsReader {

    public static Date readTimestamp(BBuffer rb) {
        var readBuffer = rb.getBytes(8);
        var longVal = (((long) readBuffer[0] << 56) +
                ((long) (readBuffer[1] & 255) << 48) +
                ((long) (readBuffer[2] & 255) << 40) +
                ((long) (readBuffer[3] & 255) << 32) +
                ((long) (readBuffer[4] & 255) << 24) +
                ((readBuffer[5] & 255) << 16) +
                ((readBuffer[6] & 255) << 8) +
                ((readBuffer[7] & 255) << 0));
        return new Date(longVal * 1000);
    }

    public static Object readField(BBuffer in) {
        Object value = null;
        var type = (char) in.get();
        switch (type) {
            case 'S':
                value = LongStringHelper.read(in);
                break;
            case 'I':
                value = in.getInt();
                break;
            case 'i':
                value = in.getInt();
                break;
            case 'D': {
                int scale = in.get();
                long unscaled = in.getLong();
                value = new BigDecimal(new BigInteger(String.valueOf(unscaled)), scale);
            }
            break;
            case 'T':
                value = readTimestamp(in);
                break;
            case 'F':
                value = readTable(in);
                break;
            case 'A':
                value = readArray(in);
                break;
            case 'b':
                value = in.get();
                break;
            case 'B':
                value = in.get();
                break;
            case 'd':
                value = in.getDouble();
                break;
            case 'f':
                value = in.getFloat();
                break;
            case 'l':
                value = in.getLong();
                break;
            case 's':
                value = in.getShort();
                break;
            case 'u':
                value = in.getShort();
                break;
            case 't':
                value = in.get() == 1;
                break;
            case 'x': {
                var contentLength = in.getInt();
                value = in.getBytes(contentLength);
            }
            break;
            case 'V':
                break;
            default:
                throw new TPMProtocolException
                        ("Unrecognised type in table field: " + type);
        }
        return value;

    }

    public static List<Object> readArray(BBuffer in) {
        var size = in.getInt();
        var lastPos = size + in.getPosition();
        List<Object> list = new ArrayList<>();
        while (in.getPosition() < lastPos) {
            Object value = readField(in);
            list.add(value);
        }
        return list;
    }

    public static Map<String, Object> readTable(BBuffer in) {
        var size = in.getInt();
        var lastPos = size + in.getPosition();
        Map<String, Object> table = new HashMap<>();
        while (in.getPosition() < lastPos) {
            String name = ShortStringHelper.read(in);
            Object value = readField(in);
            if (!table.containsKey(name))
                table.put(name, value);
        }
        return table;
    }
}
