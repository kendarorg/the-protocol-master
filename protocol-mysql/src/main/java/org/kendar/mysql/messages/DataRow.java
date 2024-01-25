package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.sql.jdbc.ProxyMetadata;

import java.util.Base64;
import java.util.List;

public class DataRow extends MySQLReturnMessage {
    private final List<String> rows;
    private final List<ProxyMetadata> metadata;

    public DataRow(List<String> rows, List<ProxyMetadata> metadata) {

        this.rows = rows;
        this.metadata = metadata;
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            var md = metadata.get(i);
            if (row == null) {
                resultBuffer.write((byte) 0xFB);
            } else {
                if(md.isByteData()){
                    resultBuffer.writeWithLength(Base64.getDecoder().decode(row.getBytes()));
                }else {
                    resultBuffer.writeWithLength(row.getBytes());
                }
            }
        }
    }
}
