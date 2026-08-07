package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsDataType;
import org.kendar.mssql.constants.TdsTokenType;

/**
 * RETURNVALUE carrying an integer output parameter (used for the
 * prepared statement handles of sp_prepare/sp_prepexec)
 */
public class ReturnValueToken extends TdsToken {
    private final int ordinal;
    private final String name;
    private final long value;

    public ReturnValueToken(int ordinal, String name, long value) {
        this.ordinal = ordinal;
        this.name = name;
        this.value = value;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        buffer.write(TdsTokenType.RETURNVALUE);
        buffer.writeUShortLE(ordinal);
        buffer.writeBVarchar(name);
        buffer.write((byte) 0x01); //output param
        buffer.writeUIntLE(0); //user type
        buffer.writeUShortLE(0); //flags
        buffer.write((byte) TdsDataType.INTN);
        buffer.write((byte) 4);
        buffer.write((byte) 4);
        buffer.writeUIntLE(value);
    }
}
