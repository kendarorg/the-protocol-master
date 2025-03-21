package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.Language;
import org.kendar.mysql.constants.MySQLType;
import org.kendar.sql.jdbc.ProxyMetadata;

import java.sql.JDBCType;

public class ColumnDefinition extends MySQLReturnMessage {
    private final ProxyMetadata field;
    private final Language language;
    private final boolean binary;

    public ColumnDefinition(ProxyMetadata fields, Language language, boolean binary) {

        this.field = fields;
        this.language = language;
        this.binary = binary;
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {
        resultBuffer.writeWithLength("def".getBytes());//catalog
        resultBuffer.writeWithLength("def".getBytes());//schema
        resultBuffer.writeWithLength("def".getBytes());//table
        resultBuffer.writeWithLength("def".getBytes());//table
        resultBuffer.writeWithLength(field.getColumnName().getBytes());//label
        resultBuffer.writeWithLength(field.getColumnName().getBytes());//name
        resultBuffer.writeLength(0x0c);
        resultBuffer.writeUB2(language.getValue());
        resultBuffer.writeUB4(getMaxColumnDisplaySize(field.getColumnType()));
        if (binary) {
            resultBuffer.write((byte) toMysql(field.getColumnType()));
        } else {
            resultBuffer.write((byte) MySQLType.MYSQL_TYPE_VAR_STRING.getValue());
        }
        resultBuffer.writeUB2(0x00);
        if (field.getPrecision() > 0) {
            resultBuffer.write((byte) field.getPrecision());
        } else {
            resultBuffer.write((byte) 0x00);
        }
        resultBuffer.write((byte) 0x00);
        resultBuffer.write((byte) 0x00);

    }

    private long getMaxColumnDisplaySize(JDBCType columnType) {
        return switch (columnType) {
            case BOOLEAN, BIT -> 1;
            case BIGINT, INTEGER, SMALLINT, TINYINT, DOUBLE, FLOAT, DATE, TIME, TIME_WITH_TIMEZONE, TIMESTAMP,
                 TIMESTAMP_WITH_TIMEZONE -> 32;
            default -> 999999999L;
        };
    }

    private int toMysql(JDBCType columnType) {
        MySQLType value = switch (columnType) {
            case BOOLEAN, BIT -> MySQLType.MYSQL_TYPE_BIT;
            case BIGINT -> MySQLType.MYSQL_TYPE_LONGLONG;
            case INTEGER -> MySQLType.MYSQL_TYPE_LONG;
            case SMALLINT -> MySQLType.MYSQL_TYPE_SHORT;
            case TINYINT -> MySQLType.MYSQL_TYPE_TINY;
            case DOUBLE -> MySQLType.MYSQL_TYPE_DOUBLE;
            case FLOAT -> MySQLType.MYSQL_TYPE_FLOAT;
            case DATE -> MySQLType.MYSQL_TYPE_DATE;
            case TIME, TIME_WITH_TIMEZONE -> MySQLType.MYSQL_TYPE_TIME;
            case TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> MySQLType.MYSQL_TYPE_TIMESTAMP;
            default -> MySQLType.MYSQL_TYPE_VAR_STRING;
        };
        return value.getValue();
    }
}
