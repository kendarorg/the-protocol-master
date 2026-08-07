package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsTokenType;

public class ReturnStatusToken extends TdsToken {
    private final int value;

    public ReturnStatusToken(int value) {
        this.value = value;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        buffer.write(TdsTokenType.RETURNSTATUS);
        buffer.writeUIntLE(value);
    }
}
