package org.kendar.postgres.executor.converters;

import org.apache.commons.beanutils.ConvertUtils;
import org.kendar.protocol.context.NetworkProtoContext;
import org.postgresql.util.ByteConverter;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.regex.Pattern;

public class PostgresDataConverter {


    private static final Pattern simpleTzDay = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2})\\s*([+\\-]*)([0-9]*)");
    private static final Pattern simpleTzTime = Pattern.compile("([0-9]{2}:[0-9]{2}:[0-9]{2})\\s*([+\\-]*)([0-9]*)");
    private static final Pattern simpleTzTimestamp = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2})" +
            "\\s*([0-9]{2}:[0-9]{2}:[0-9]{2})" +
            "([\\.]{0,1})([0-9]+)\\s*([+\\-]*)([0-9]*)");

    public static Object bytesToJava(byte[] bytes, String simpleClassName, NetworkProtoContext protoContext, boolean isOutput) {
        Object value = null;

        if (!isOutput) {
            switch (simpleClassName) {
                case "BigDecimal":
                    value = new BigDecimal(ByteConverter.numeric(bytes).toString());
                    break;
                case "Double":
                    value = ByteConverter.float8(bytes, 0);
                    break;
                case "Float":
                    value = ByteConverter.float4(bytes, 0);
                    break;
                case "Long":
                    value = ByteConverter.int8(bytes, 0);
                    break;
                case "Integer":
                    value = ByteConverter.int4(bytes, 0);
                    break;
                case "Short":
                    value = ByteConverter.int2(bytes, 0);
                    break;
                case "Boolean":
                    value = ByteConverter.bool(bytes, 0);
                    break;
                case "String":
                    var bb = protoContext.buildBuffer();
                    bb.write(bytes);
                    bb.setPosition(0);
                    value = bb.getUtf8String();
                    break;
                case "[B":
                case "byte[]":
                default:
                    value = Base64.getEncoder().encodeToString(bytes);
                    break;
            }
        }
        return value;
    }

    public static Object convert(String toConvert, Class<?> clazz) {
        if (clazz == java.util.Date.class || clazz == java.sql.Date.class) {
            var matcher = simpleTzDay.matcher(toConvert);
            if (matcher.matches()) {
                return Date.valueOf(matcher.group(1));
            }
        } else if (clazz == java.sql.Time.class) {
            var matcher = simpleTzTime.matcher(toConvert);
            if (matcher.matches()) {
                return Time.valueOf(matcher.group(1));
            }
        } else if (clazz == java.sql.Timestamp.class) {
            var matcher = simpleTzTimestamp.matcher(toConvert);
            if (matcher.matches()) {
                var stringed = matcher.group(1) + " " + matcher.group(2) + matcher.group(3) + matcher.group(4);
                return Timestamp.valueOf(stringed);
            }
        }
        return ConvertUtils.convert(toConvert, clazz);
    }
    /*
    public static int toPgwType(int columnType) throws SQLException {
        switch (columnType) {
            case Types.BIGINT:
                return TypesOids.Int8;
            case Types.ARRAY:
                return TypesOids.TsVector;
            case Types.BIT:
                return TypesOids.Bool;
            case Types.BINARY:
                return TypesOids.Bytea;
            case Types.BLOB:
                return TypesOids.Varbit;
            case Types.CHAR:
                return TypesOids.BPChar;
            case Types.CLOB:
                return TypesOids.Varchar;
            case Types.DATE:
                return TypesOids.Date;
            case Types.DECIMAL:
                return TypesOids.Numeric;
            case Types.DOUBLE:
                return TypesOids.Float8;
            case Types.INTEGER:
                return TypesOids.Int4;
            case Types.LONGNVARCHAR:
                return TypesOids.Varchar;
            case Types.LONGVARBINARY:
                return TypesOids.Varbit;
            case Types.VARCHAR:
                return TypesOids.Varchar;
            case Types.VARBINARY:
                return TypesOids.Varbit;
            case Types.NCHAR:
                return TypesOids.Varchar;
            case Types.NCLOB:
                return TypesOids.Varchar;
            case Types.NUMERIC:
                return TypesOids.Numeric;
            case Types.REAL:
                return TypesOids.Numeric;
            case Types.SMALLINT:
                return TypesOids.Int2;
            case Types.TIME:
                return TypesOids.Time;
            case Types.TIME_WITH_TIMEZONE:
                return TypesOids.TimeTz;
            case Types.TIMESTAMP:
                return TypesOids.Timestamp;
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return TypesOids.TimestampTz;
            case Types.TINYINT:
                return TypesOids.Int2;
            case Types.SQLXML:
                return TypesOids.Varchar;
            case Types.ROWID:
                return TypesOids.Int8;
            case Types.BOOLEAN:
                return TypesOids.Bool;
            case 0:
                return TypesOids.Void;
            default:
                throw new SQLException("NOT RECOGNIZED COLUMN TYPE " + columnType);
        }

    }

    public static Class<?> fromPgwType(int columnType) {
        switch (columnType) {
            case TypesOids.Int8:
                return Long.class;
            case TypesOids.Int4:
                return Integer.class;
            case TypesOids.Int2:
                return Short.class;
            case TypesOids.TsVector:
                return Array.class;
            case TypesOids.Bool:
                return Boolean.class;
            case TypesOids.Bytea:
            case TypesOids.Varbit:
                return byte[].class;
            case TypesOids.BPChar:
            case TypesOids.Varchar:
            case TypesOids.Text:
            case TypesOids.Json:
            case TypesOids.Xml:
                return String.class;
            case TypesOids.Numeric:
            case TypesOids.Money:
                return BigDecimal.class;
            case TypesOids.Float4:
                return Float.class;
            case TypesOids.Float8:
                return Double.class;
            case TypesOids.Date:
                return Date.class;
            case TypesOids.Time:
            case TypesOids.TimeTz:
                return Time.class;
            case TypesOids.Timestamp:
            case TypesOids.TimestampTz:
                return Timestamp.class;
            case TypesOids.Void:
                return Void.class;
            default:
                throw new TPMException("NOT RECOGNIZED COLUMN TYPE " + columnType);
        }
    }




    public static boolean isByteOut(String clName){
        var ns = clName.split("\\.");
        var name = ns[ns.length-1].toLowerCase(Locale.ROOT);
        switch(name){
            case("[b"):
            case("[c"):
            case("byte"):
                return true;
            default:
                return false;
        }
    }*/

}
