package org.kendar.mysql.constants;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by zcg on 2017/4/4.
 */
public enum MySQLType {
    MYSQL_TYPE_DECIMAL(0x00),
    MYSQL_TYPE_TINY(0x01),
    MYSQL_TYPE_SHORT(0x02),
    MYSQL_TYPE_LONG(0x03),
    MYSQL_TYPE_FLOAT(0x04),
    MYSQL_TYPE_DOUBLE(0x05),
    MYSQL_TYPE_NULL(0x06),
    MYSQL_TYPE_TIMESTAMP(0x07),
    MYSQL_TYPE_LONGLONG(0x08),
    MYSQL_TYPE_INT24(0x09),
    MYSQL_TYPE_DATE(0x0a),
    MYSQL_TYPE_TIME(0x0b),
    MYSQL_TYPE_DATETIME(0x0c),
    MYSQL_TYPE_YEAR(0x0d),
    MYSQL_TYPE_NEWDATE(0x0e),
    MYSQL_TYPE_VARCHAR(0x0f),
    MYSQL_TYPE_BIT(0x10),
    MYSQL_TYPE_TIMESTAMP2(0x11),
    MYSQL_TYPE_DATETIME2(0x12),
    MYSQL_TYPE_TIME2(0x13),
    MYSQL_TYPE_NEWDECIMAL(0xf6),
    MYSQL_TYPE_ENUM(0xf7),
    MYSQL_TYPE_SET(0xf8),
    MYSQL_TYPE_TINY_BLOB(0xf9),
    MYSQL_TYPE_MEDIUM_BLOB(0xfa),
    MYSQL_TYPE_LONG_BLOB(0xfb),
    MYSQL_TYPE_BLOB(0xfc),
    MYSQL_TYPE_VAR_STRING(0xfd),
    MYSQL_TYPE_STRING(0xfe),
    MYSQL_TYPE_GEOMETRY(0xff);
    private static final Map<Integer, MySQLType> BY_INT = new HashMap<>();

    static {
        for (MySQLType e : values()) {
            BY_INT.put(e.value, e);
        }
    }

    private final int value;

    MySQLType(int value) {

        this.value = value;
    }

    public static MySQLType of(int value) {
        return BY_INT.get(value);
    }

    public int getValue() {
        return value;
    }

//    public static Object data(int fieldType, MySQLMessage m) {
//
//        switch (fieldType) {
//            case MYSQL_TYPE_VARCHAR:
//            case MYSQL_TYPE_BIT:
//            case MYSQL_TYPE_ENUM:
//            case MYSQL_TYPE_SET:
//            case MYSQL_TYPE_TINY_BLOB:
//            case MYSQL_TYPE_MEDIUM_BLOB:
//            case MYSQL_TYPE_LONG_BLOB:
//            case MYSQL_TYPE_BLOB:
//            case MYSQL_TYPE_VAR_STRING:
//            case MYSQL_TYPE_STRING:
//            case MYSQL_TYPE_GEOMETRY:
////            case MYSQL_TYPE_OLDDECIMAL:
//            case MYSQL_TYPE_DECIMAL:
//                return m.readStringWithLength();
//            case MYSQL_TYPE_TIME:
//            case MYSQL_TYPE_DATE:
//            case MYSQL_TYPE_DATETIME:
//                return m.readDate();
//            case MYSQL_TYPE_TIMESTAMP:
//                return m.readTime();
//            case MYSQL_TYPE_LONGLONG:
//                return m.readLong();
//            case MYSQL_TYPE_LONG:
//                return m.readInt();
////            case MYSQL_TYPE_SMALLINT:
//            case MYSQL_TYPE_YEAR:
//                return m.readUB2();
//            case MYSQL_TYPE_TINY:
//                return m.read();
//            case MYSQL_TYPE_DOUBLE:
//                return m.readDouble();
//            case MYSQL_TYPE_FLOAT:
//                return m.readFloat();
//            default:
//                return null;
//        }
//    }
}
