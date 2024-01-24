package org.kendar.mysql.messages;

import org.kendar.buffers.BBufferUtils;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.sql.jdbc.ProxyMetadata;

import java.util.Base64;
import java.util.List;

public class BinaryDataRow extends MySQLReturnMessage {
    private final List<String> rows;
    private final List<ProxyMetadata> metadata;

    public BinaryDataRow(List<String> rows, List<ProxyMetadata> metadata) {

        this.rows = rows;
        this.metadata = metadata;
    }

    private static void buildMysqlBinaryType(MySQLBBuffer resultBuffer, ProxyMetadata md, String row) {


        switch (md.getColumnType()){
            case BOOLEAN:
            case BIT:
                resultBuffer.write((byte) (Boolean.parseBoolean(row)?0x01:0x00));
                break;

            case DATE:
            case TIME:
            case TIME_WITH_TIMEZONE:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
            case FLOAT:
            case DOUBLE:
            case BIGINT:
            case INTEGER:
            case TINYINT:
            case SMALLINT:
            case CLOB:
            case LONGNVARCHAR:
            case LONGVARCHAR:
            case NCHAR:
            case NCLOB:
            case VARCHAR:
            case DECIMAL:
            case CHAR:
                resultBuffer.writeWithLength(row.getBytes());
                break;
            default:
                resultBuffer.writeWithLength(Base64.getDecoder().decode(row));
                break;
        }
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {

        resultBuffer.write((byte) 0x00);
        resultBuffer.write(generateNullBitmap());
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            var md = metadata.get(i);
            if (row != null) {
                buildMysqlBinaryType(resultBuffer, md, row);
            }
        }
    }

    private byte[] generateNullBitmap() {
        int nullBitmapSize = (rows.size() + 7+2) / 8;
        var newAr = new byte[nullBitmapSize];
        for (int i = 0; i < rows.size(); i++) {
            if(rows.get(i)==null){
                BBufferUtils.setBit(newAr,i);
            }
        }
        return newAr;
    }
}
