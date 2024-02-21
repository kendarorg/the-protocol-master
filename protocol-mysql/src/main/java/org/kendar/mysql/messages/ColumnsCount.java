package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.sql.jdbc.ProxyMetadata;

import java.util.List;

public class ColumnsCount extends MySQLReturnMessage {
    private final List<ProxyMetadata> fields;

    public ColumnsCount(List<ProxyMetadata> fields) {

        this.fields = fields;
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {
        resultBuffer.writeLength(fields.size());
    }
}
