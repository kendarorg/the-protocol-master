package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.sql.jdbc.ProxyMetadata;

import java.util.List;

public class ComStmtPrepareOk extends MySQLReturnMessage {
    private final List<ProxyMetadata> fields;
    private final int statementId;

    public ComStmtPrepareOk(List<ProxyMetadata> fields, int statementId) {

        this.fields = fields;
        this.statementId = statementId;
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {
        var parameterFields = fields.stream()
                .filter(f -> f.getColumnName().equalsIgnoreCase("?"))
                .count();
        var resultFields = fields.size() - parameterFields;
        resultBuffer.write((byte) 0x00);
        resultBuffer.writeUB4(statementId);
        resultBuffer.writeUB2((int) resultFields);
        resultBuffer.writeUB2((int) parameterFields);
        resultBuffer.write((byte) 0x00);
        resultBuffer.writeUB2((int) 0x00);
    }
}
