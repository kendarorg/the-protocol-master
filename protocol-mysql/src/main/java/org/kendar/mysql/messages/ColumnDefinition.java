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


        if (field.getCatalogName() == null || field.getCatalogName().isEmpty()) {
            resultBuffer.writeWithLength("def".getBytes());
        } else {
            resultBuffer.writeWithLength(field.getCatalogName().getBytes());
        }
        resultBuffer.writeWithLength(field.getSchemaName().getBytes());
        resultBuffer.writeWithLength(field.getTableName().getBytes());
        resultBuffer.writeWithLength(field.getTableName().getBytes());
        if (field.getColumnLabel() != null && !field.getColumnLabel().isEmpty()) {
            resultBuffer.writeWithLength(field.getColumnLabel().getBytes());
        } else {
            resultBuffer.writeWithLength(field.getColumnName().getBytes());
        }
        resultBuffer.writeWithLength(field.getColumnLabel().getBytes());
        resultBuffer.writeLength(0x0c);
        resultBuffer.writeUB2(language.getValue());
        resultBuffer.writeUB4(field.getColumnDisplaySize());

        if (binary) {
            resultBuffer.write((byte) toMysql(field.getColumnType()));
        } else {
            resultBuffer.write((byte) MySQLType.MYSQL_TYPE_VAR_STRING.getValue());
        }
        resultBuffer.writeUB2(0x00);
        //TODO VERIFIY PRECISION https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_column_definition.html
        if (field.getPrecision() > 0) {
            resultBuffer.write((byte) field.getPrecision());
        } else {
            resultBuffer.write((byte) 0x00);
        }
        resultBuffer.write((byte) 0x00);
        resultBuffer.write((byte) 0x00);
    }

    private int toMysql(JDBCType columnType) {
        MySQLType value = MySQLType.MYSQL_TYPE_VARCHAR;
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
