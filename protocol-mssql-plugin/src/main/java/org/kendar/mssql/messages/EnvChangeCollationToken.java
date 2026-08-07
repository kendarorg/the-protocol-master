package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.EnvChangeType;
import org.kendar.mssql.constants.TdsTokenType;

/**
 * ENVCHANGE type 7: the SQL collation of the connection, needed by the
 * clients to encode the character parameters they send
 */
public class EnvChangeCollationToken extends TdsToken {
    private final byte[] collation;

    public EnvChangeCollationToken(byte[] collation) {
        this.collation = collation;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        var length = 1 + (1 + collation.length) + 1;
        buffer.write(TdsTokenType.ENVCHANGE);
        buffer.writeUShortLE(length);
        buffer.write(EnvChangeType.COLLATION);
        buffer.write((byte) collation.length);
        buffer.write(collation);
        buffer.write((byte) 0);
    }
}
