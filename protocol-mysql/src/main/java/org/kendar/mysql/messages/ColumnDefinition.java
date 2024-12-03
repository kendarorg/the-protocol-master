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
        long value;
        switch (columnType) {
            case BOOLEAN:
            case BIT:
                value = 1;
                break;
            case BIGINT:
            case INTEGER:
            case SMALLINT:
            case TINYINT:
            case DOUBLE:
            case FLOAT:
            case DATE:
            case TIME:
            case TIME_WITH_TIMEZONE:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
                value = 32;
                break;
            default:
                value = 999999999L;
                break;
        }
        return value;
    }

    private int toMysql(JDBCType columnType) {
        MySQLType value;
        switch (columnType) {
            case BOOLEAN:
            case BIT:
                value = MySQLType.MYSQL_TYPE_BIT;
                break;
            case BIGINT:
                value = MySQLType.MYSQL_TYPE_LONGLONG;
                break;
            case INTEGER:
                value = MySQLType.MYSQL_TYPE_LONG;
                break;
            case SMALLINT:
                value = MySQLType.MYSQL_TYPE_SHORT;
                break;
            case TINYINT:
                value = MySQLType.MYSQL_TYPE_TINY;
                break;
            case DOUBLE:
                value = MySQLType.MYSQL_TYPE_DOUBLE;
                break;
            case FLOAT:
                value = MySQLType.MYSQL_TYPE_FLOAT;
                break;
            case DATE:
                value = MySQLType.MYSQL_TYPE_DATE;
                break;
            case TIME:
            case TIME_WITH_TIMEZONE:
                value = MySQLType.MYSQL_TYPE_TIME;
                break;
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
                value = MySQLType.MYSQL_TYPE_TIMESTAMP;
                break;
            default:
                value = MySQLType.MYSQL_TYPE_VAR_STRING;
                break;
        }
        return value.getValue();
    }
}
