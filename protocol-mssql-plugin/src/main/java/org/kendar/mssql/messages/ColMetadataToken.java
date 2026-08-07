package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsDataType;
import org.kendar.mssql.constants.TdsTokenType;
import org.kendar.sql.jdbc.ProxyMetadata;

import java.util.List;

/**
 * COLMETADATA: phase 1 simplification, everything is exposed as
 * NVARCHAR(4000) or VARBINARY(8000) since the jdbc layer serializes
 * rows as strings/base64 (same approach as the postgres text results)
 */
public class ColMetadataToken extends TdsToken {
    //Latin1_General collation
    public static final byte[] DEFAULT_COLLATION = new byte[]{0x09, 0x04, (byte) 0xD0, 0x00, 0x34};
    private final List<ProxyMetadata> fields;

    public ColMetadataToken(List<ProxyMetadata> fields) {
        this.fields = fields;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        buffer.write(TdsTokenType.COLMETADATA);
        buffer.writeUShortLE(fields.size());
        for (var field : fields) {
            buffer.writeUIntLE(0); //user type
            buffer.writeUShortLE(0x0009); //nullable, computed
            if (field.isByteData()) {
                buffer.write((byte) TdsDataType.BIGVARBIN);
                buffer.writeUShortLE(8000);
            } else {
                buffer.write((byte) TdsDataType.NVARCHAR);
                buffer.writeUShortLE(8000);
                buffer.writeCollation(DEFAULT_COLLATION);
            }
            buffer.writeBVarchar(field.getColumnName());
        }
    }
}
