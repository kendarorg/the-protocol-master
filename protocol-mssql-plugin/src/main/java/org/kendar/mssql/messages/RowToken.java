package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsTokenType;
import org.kendar.sql.jdbc.ProxyMetadata;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class RowToken extends TdsToken {
    private static final int CHARBIN_NULL = 0xFFFF;
    private final List<String> values;
    private final List<ProxyMetadata> fields;

    public RowToken(List<String> values, List<ProxyMetadata> fields) {
        this.values = values;
        this.fields = fields;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        buffer.write(TdsTokenType.ROW);
        for (var i = 0; i < values.size(); i++) {
            var value = values.get(i);
            var field = fields.get(i);
            if (value == null) {
                buffer.writeUShortLE(CHARBIN_NULL);
            } else if (field.isByteData()) {
                var bytes = Base64.getDecoder().decode(value);
                buffer.writeUShortLE(bytes.length);
                buffer.write(bytes);
            } else {
                var bytes = value.getBytes(StandardCharsets.UTF_16LE);
                buffer.writeUShortLE(bytes.length);
                buffer.write(bytes);
            }
        }
    }
}
