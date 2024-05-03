package org.kendar.amqp.v09.utils;

import org.kendar.buffers.BBuffer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

public class FieldsWriter {

    @SuppressWarnings("DuplicateBranchesInSwitch")
    public static void writeField(Object value, BBuffer io) {
        if (value == null) {
            io.write((byte) 'V');
            return;
        }
        switch (value.getClass().getSimpleName()) {
            case "Boolean":
                io.write((byte) 't');
                io.write((byte) (((boolean) value) ? 0x01 : 0x02));
                return;
            case "Byte":
                io.write((byte) 'b');
                io.write((byte) value);
            case "Short":
                io.write((byte) 's');
                io.writeShort((short) value);
                return;
            case "Integer":
                io.write((byte) 'I');
                io.writeInt((int) value);
                return;
            case "Long":
                io.write((byte) 'l');
                io.writeLong((long) value);
                return;
            case "BigDecimal": {
                io.write((byte) 'D');
                BigDecimal decimal = (BigDecimal) value;
                io.write((byte) decimal.scale());
                BigInteger unscaled = decimal.unscaledValue();
                io.writeLong(decimal.unscaledValue().intValue());
                return;
            }
            case "Double":
                io.write((byte) 'd');
                io.writeDouble((double) value);
                return;
            case "Float":
                io.write((byte) 'f');
                io.writeFloat((float) value);
                return;
            case "[B":
            case "byte[]":
            case "Byte[]": {
                io.write((byte) 'x');
                var bv = ((byte[]) value).length;
                io.writeLong((long) bv);
                io.write((byte[]) value);
                return;
            }
            case "String": {

                io.write((byte) 'S');
                new LongStringHelper((String) value).write(io);
                return;
            }
            case "Date":
                io.write((byte) 'T');
                io.writeLong(((Timestamp) value).getTime() / 1000);
                break;
            case "Map":
            case "HashMap": {
                io.write((byte) 'F');
                writeTable((Map<String, Object>) value, io);
                return;
            }
            case "List":
            case "ArrayList": {
                io.write((byte) 'A');
                writeArray((Object[]) value, io);
                return;
            }
            case "Object[]": {
                io.write((byte) 'A');
                writeArray((Object[]) value, io);
                return;
            }
            default:
                throw new RuntimeException("UNSUPPORTED TYPE " + value.getClass());
        }
    }

    private static void writeArray(Object[] value, BBuffer io) {
        if (value == null) {
            io.writeInt((int) 0);
        } else {
            var countPos = io.getPosition();
            io.writeInt((int) 0);
            var startPos = io.getPosition();
            for (Object item : value) {
                writeField(item, io);
            }
            var endPos = io.getPosition();
            io.writeInt(endPos - startPos, countPos);
        }
    }

    public static void writeTable(Map<String, Object> table, BBuffer io) {
        if (table == null) {
            // Convenience.
            io.writeInt((int) 0);
        } else {
            var countPos = io.getPosition();
            io.writeInt((int) 0);
            var startPos = io.getPosition();
            for (Map.Entry<String, Object> entry : table.entrySet()) {
                new ShortStringHelper(entry.getKey()).write(io);
                Object value = entry.getValue();
                writeField(value, io);
            }
            var endPos = io.getPosition();
            io.writeInt(endPos - startPos, countPos);
        }

    }

    public static void writeTimestamp(BBuffer rb, Date timestamp) {
        var longLong = timestamp.getTime() / 1000;
        rb.writeLong(longLong);
    }
}
