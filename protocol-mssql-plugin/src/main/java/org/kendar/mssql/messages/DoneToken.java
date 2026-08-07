package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsTokenType;

public class DoneToken extends TdsToken {
    private final byte tokenType;
    private final int status;
    private final long rowCount;
    private int curCmd;

    public DoneToken(int status, long rowCount) {
        this(TdsTokenType.DONE, status, rowCount);
    }

    public DoneToken(byte tokenType, int status, long rowCount) {
        this.tokenType = tokenType;
        this.status = status;
        this.rowCount = rowCount;
    }

    /**
     * The clients honor the row count only when the current command is a
     * DML/DDL one (INSERT 0xC3, DELETE 0xC2, UPDATE 0xC5, DDL 0xF0...)
     */
    public DoneToken withCurCmd(int curCmd) {
        this.curCmd = curCmd;
        return this;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        buffer.write(tokenType);
        buffer.writeUShortLE(status);
        buffer.writeUShortLE(curCmd);
        buffer.writeULongLE(rowCount);
    }
}
