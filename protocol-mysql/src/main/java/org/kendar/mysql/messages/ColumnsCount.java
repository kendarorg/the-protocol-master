package org.kendar.mysql.messages;

import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.Language;
import org.kendar.sql.jdbc.ProxyMetadata;

import java.util.List;
import java.util.Optional;

public class ColumnsCount extends MySQLReturnMessage {
    private final List<ProxyMetadata> fields;
    private final Optional<Boolean> metadataFollows;
    private final Language language;

    public ColumnsCount(List<ProxyMetadata> fields, Optional<Boolean> metadataFollows, Language language) {

        this.fields = fields;
        this.metadataFollows = metadataFollows;
        this.language = language;
    }

    @Override
    protected void writeResponse(MySQLBBuffer resultBuffer) {
        resultBuffer.writeLength(fields.size());
    }
}
