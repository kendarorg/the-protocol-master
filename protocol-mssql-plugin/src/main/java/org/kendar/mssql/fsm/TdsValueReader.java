package org.kendar.mssql.fsm;

import org.kendar.exceptions.TPMProtocolException;
import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsDataType;
import org.kendar.sql.jdbc.BindingParameter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.JDBCType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;

/**
 * Decodes the TYPE_INFO + value of an RPC parameter into a
 * BindingParameter with the string formats understood by the shared
 * jdbc proxy (Timestamp/Date/Time.valueOf and friends)
 */
public class TdsValueReader {

    private static final LocalDate TDS_EPOCH = LocalDate.of(1, 1, 1);
    private static final LocalDate SQL_EPOCH = LocalDate.of(1900, 1, 1);

    public static BindingParameter readParam(MssqlBBuffer buffer) {
        var type = buffer.get() & 0xFF;
        switch (type) {
            case TdsDataType.INTN:
                return readIntN(buffer, buffer.get() & 0xFF);
            case TdsDataType.INT1:
                return new BindingParameter(Long.toString(buffer.get() & 0xFF), false, JDBCType.TINYINT);
            case TdsDataType.INT2:
                return new BindingParameter(Long.toString((short) buffer.readUShortLE()), false, JDBCType.SMALLINT);
            case TdsDataType.INT4:
                return new BindingParameter(Long.toString((int) buffer.readUIntLE()), false, JDBCType.INTEGER);
            case TdsDataType.INT8:
                return new BindingParameter(Long.toString(buffer.readULongLE()), false, JDBCType.BIGINT);
            case TdsDataType.BITN: {
                buffer.get(); //max length
                var len = buffer.get() & 0xFF;
                if (len == 0) return new BindingParameter(null, false, JDBCType.BIT);
                return new BindingParameter(Boolean.toString(buffer.get() != 0), false, JDBCType.BIT);
            }
            case TdsDataType.BIT:
                return new BindingParameter(Boolean.toString(buffer.get() != 0), false, JDBCType.BIT);
            case TdsDataType.FLTN: {
                buffer.get(); //max length
                var len = buffer.get() & 0xFF;
                return readFloat(buffer, len);
            }
            case TdsDataType.FLT4:
                return readFloat(buffer, 4);
            case TdsDataType.FLT8:
                return readFloat(buffer, 8);
            case TdsDataType.DECIMALN:
            case TdsDataType.NUMERICN: {
                buffer.get(); //max length
                buffer.get(); //precision
                var scale = buffer.get() & 0xFF;
                var len = buffer.get() & 0xFF;
                if (len == 0) return new BindingParameter(null, false, JDBCType.NUMERIC);
                var sign = buffer.get() & 0xFF;
                var magnitude = readLeMagnitude(buffer, len - 1);
                var value = new BigDecimal(sign == 1 ? magnitude : magnitude.negate(), scale);
                return new BindingParameter(value.toPlainString(), false, JDBCType.NUMERIC);
            }
            case TdsDataType.NVARCHAR:
            case TdsDataType.NCHAR: {
                var maxLen = buffer.readUShortLE();
                buffer.getBytes(5); //collation
                var data = maxLen == 0xFFFF ? readPlp(buffer) : readShortLenValue(buffer);
                if (data == null) return new BindingParameter(null, false, JDBCType.VARCHAR);
                return new BindingParameter(new String(data, StandardCharsets.UTF_16LE), false, JDBCType.VARCHAR);
            }
            case TdsDataType.BIGVARCHR:
            case TdsDataType.BIGCHAR: {
                var maxLen = buffer.readUShortLE();
                buffer.getBytes(5); //collation
                var data = maxLen == 0xFFFF ? readPlp(buffer) : readShortLenValue(buffer);
                if (data == null) return new BindingParameter(null, false, JDBCType.VARCHAR);
                return new BindingParameter(new String(data, StandardCharsets.UTF_8), false, JDBCType.VARCHAR);
            }
            case TdsDataType.BIGVARBIN:
            case TdsDataType.BIGBINARY: {
                var maxLen = buffer.readUShortLE();
                var data = maxLen == 0xFFFF ? readPlp(buffer) : readShortLenValue(buffer);
                if (data == null) return new BindingParameter(null, true, JDBCType.VARBINARY);
                return new BindingParameter(Base64.getEncoder().encodeToString(data), true, JDBCType.VARBINARY);
            }
            case TdsDataType.GUID: {
                buffer.get(); //max length
                var len = buffer.get() & 0xFF;
                if (len == 0) return new BindingParameter(null, false, JDBCType.CHAR);
                return new BindingParameter(readGuid(buffer), false, JDBCType.CHAR);
            }
            case TdsDataType.DATEN: {
                var len = buffer.get() & 0xFF;
                if (len == 0) return new BindingParameter(null, false, JDBCType.DATE);
                var days = readLeUnsigned(buffer, 3);
                var date = TDS_EPOCH.plusDays(days);
                return new BindingParameter(formatDate(date), false, JDBCType.DATE);
            }
            case TdsDataType.TIMEN: {
                var scale = buffer.get() & 0xFF;
                var len = buffer.get() & 0xFF;
                if (len == 0) return new BindingParameter(null, false, JDBCType.TIME);
                var units = readLeUnsigned(buffer, len);
                var time = timeFromUnits(units, scale);
                return new BindingParameter(String.format("%02d:%02d:%02d",
                        time.getHour(), time.getMinute(), time.getSecond()), false, JDBCType.TIME);
            }
            case TdsDataType.DATETIME2N: {
                var scale = buffer.get() & 0xFF;
                var len = buffer.get() & 0xFF;
                if (len == 0) return new BindingParameter(null, false, JDBCType.TIMESTAMP);
                var units = readLeUnsigned(buffer, len - 3);
                var days = readLeUnsigned(buffer, 3);
                var date = TDS_EPOCH.plusDays(days);
                var time = timeFromUnits(units, scale);
                return new BindingParameter(formatTimestamp(LocalDateTime.of(date, time)), false, JDBCType.TIMESTAMP);
            }
            case TdsDataType.DATETIMN: {
                buffer.get(); //max length
                var len = buffer.get() & 0xFF;
                return readDateTime(buffer, len);
            }
            case TdsDataType.DATETIME:
                return readDateTime(buffer, 8);
            case TdsDataType.DATETIM4:
                return readDateTime(buffer, 4);
            case TdsDataType.MONEYN: {
                buffer.get(); //max length
                var len = buffer.get() & 0xFF;
                return readMoney(buffer, len);
            }
            case TdsDataType.MONEY:
                return readMoney(buffer, 8);
            case TdsDataType.MONEY4:
                return readMoney(buffer, 4);
            case TdsDataType.NULLTYPE:
                return new BindingParameter(null, false, JDBCType.NULL);
            default:
                throw new TPMProtocolException("Unsupported TDS parameter type 0x" + Integer.toHexString(type));
        }
    }

    private static BindingParameter readIntN(MssqlBBuffer buffer, int maxLen) {
        var len = buffer.get() & 0xFF;
        var jdbcType = switch (maxLen) {
            case 1 -> JDBCType.TINYINT;
            case 2 -> JDBCType.SMALLINT;
            case 8 -> JDBCType.BIGINT;
            default -> JDBCType.INTEGER;
        };
        if (len == 0) return new BindingParameter(null, false, jdbcType);
        long value = switch (len) {
            case 1 -> buffer.get() & 0xFF;
            case 2 -> (short) buffer.readUShortLE();
            case 4 -> (int) buffer.readUIntLE();
            default -> buffer.readULongLE();
        };
        return new BindingParameter(Long.toString(value), false, jdbcType);
    }

    private static BindingParameter readFloat(MssqlBBuffer buffer, int len) {
        if (len == 0) return new BindingParameter(null, false, JDBCType.DOUBLE);
        if (len == 4) {
            var value = Float.intBitsToFloat((int) buffer.readUIntLE());
            return new BindingParameter(Float.toString(value), false, JDBCType.FLOAT);
        }
        var value = Double.longBitsToDouble(buffer.readULongLE());
        return new BindingParameter(Double.toString(value), false, JDBCType.DOUBLE);
    }

    private static BindingParameter readDateTime(MssqlBBuffer buffer, int len) {
        if (len == 0) return new BindingParameter(null, false, JDBCType.TIMESTAMP);
        if (len == 4) {
            var days = buffer.readUShortLE();
            var minutes = buffer.readUShortLE();
            var dateTime = SQL_EPOCH.plusDays(days).atStartOfDay().plusMinutes(minutes);
            return new BindingParameter(formatTimestamp(dateTime), false, JDBCType.TIMESTAMP);
        }
        var days = (int) buffer.readUIntLE();
        var ticks = buffer.readUIntLE();
        var nanos = Math.round(ticks * (1_000_000_000L / 300.0));
        var dateTime = SQL_EPOCH.plusDays(days).atStartOfDay().plusNanos(nanos);
        return new BindingParameter(formatTimestamp(dateTime), false, JDBCType.TIMESTAMP);
    }

    private static BindingParameter readMoney(MssqlBBuffer buffer, int len) {
        if (len == 0) return new BindingParameter(null, false, JDBCType.NUMERIC);
        long value;
        if (len == 4) {
            value = (int) buffer.readUIntLE();
        } else {
            var high = buffer.readUIntLE();
            var low = buffer.readUIntLE();
            value = (high << 32) | low;
        }
        return new BindingParameter(BigDecimal.valueOf(value, 4).toPlainString(), false, JDBCType.NUMERIC);
    }

    private static LocalTime timeFromUnits(long units, int scale) {
        var nanos = units;
        for (var i = scale; i < 9; i++) {
            nanos *= 10;
        }
        return LocalTime.ofNanoOfDay(nanos);
    }

    private static String formatDate(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static String formatTimestamp(LocalDateTime dateTime) {
        return String.format("%04d-%02d-%02d %02d:%02d:%02d.%09d",
                dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
                dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getNano());
    }

    private static String readGuid(MssqlBBuffer buffer) {
        var data = buffer.getBytes(16);
        return String.format(
                "%02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X",
                data[3], data[2], data[1], data[0],
                data[5], data[4],
                data[7], data[6],
                data[8], data[9],
                data[10], data[11], data[12], data[13], data[14], data[15]);
    }

    private static byte[] readShortLenValue(MssqlBBuffer buffer) {
        var len = buffer.readUShortLE();
        if (len == 0xFFFF) return null;
        return buffer.getBytes(len);
    }

    private static byte[] readPlp(MssqlBBuffer buffer) {
        var total = buffer.readULongLE();
        if (total == -1L) return null; //PLP NULL
        var result = new MssqlBBuffer();
        while (true) {
            var chunkLen = buffer.readUIntLE();
            if (chunkLen == 0) break;
            result.write(buffer.getBytes((int) chunkLen));
        }
        return result.toArray();
    }

    private static BigInteger readLeMagnitude(MssqlBBuffer buffer, int len) {
        var data = buffer.getBytes(len);
        var reversed = new byte[len];
        for (var i = 0; i < len; i++) {
            reversed[i] = data[len - 1 - i];
        }
        return new BigInteger(1, reversed);
    }

    private static long readLeUnsigned(MssqlBBuffer buffer, int len) {
        long result = 0;
        for (var i = 0; i < len; i++) {
            result |= (buffer.get() & 0xFFL) << (8 * i);
        }
        return result;
    }
}
