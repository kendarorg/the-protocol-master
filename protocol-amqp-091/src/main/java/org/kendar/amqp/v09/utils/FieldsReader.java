package org.kendar.amqp.v09.utils;

import org.kendar.buffers.BBuffer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class FieldsReader {
    public static Object readField( BBuffer in) {
        Object value = null;
        var type = (char)in.get();
        switch(type) {
            case 'S':
                value = LongStringHelper.read(in);
                break;
            case 'I':
                value = in.getInt();
                break;
            case 'i':
                value = in.getInt();
                break;
            case 'D':
            {
                int scale = in.get();
                long unscaled =in.getLong();
                value = new BigDecimal(new BigInteger(String.valueOf(unscaled)),scale);
            }
                break;
            case 'T':
                value = new Date(in.getLong()*1000);
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
                value = in.get()==1;
                break;
            case 'x':
            {
                var contentLength = in.getInt();
                value = in.getBytes(contentLength);
            }
                break;
            case 'V':
                value = null;
                break;
            default:
                throw new RuntimeException
                        ("Unrecognised type in table");
        }
        return value;

    }

    public static List<Object> readArray(BBuffer in) {
        var size = in.getInt();
        var lastPos = size+in.getPosition();
        List<Object> list = new ArrayList<>();
        while(in.getPosition()<lastPos){
            Object value = readField(in);
            list.add(value);
        }
        return list;
    }

    public static Map<String,Object> readTable(BBuffer in) {
        var size = in.getInt();
        var lastPos = size+in.getPosition();
        Map<String, Object> table = new HashMap<String, Object>();
        while(in.getPosition()<lastPos){
            String name = ShortStringHelper.read(in);
            Object value = readField(in);
            if(!table.containsKey(name))
                table.put(name, value);
        }
        return table;
    }
}
