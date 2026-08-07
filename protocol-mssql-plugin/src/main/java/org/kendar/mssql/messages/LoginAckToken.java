package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsTokenType;

import java.nio.charset.StandardCharsets;

public class LoginAckToken extends TdsToken {
    private static final String PROG_NAME = "Microsoft SQL Server";
    //TDS 7.4
    private static final byte[] TDS_VERSION = new byte[]{0x74, 0x00, 0x00, 0x04};
    private static final byte[] PROG_VERSION = new byte[]{0x10, 0x00, 0x00, 0x00};

    @Override
    public void write(MssqlBBuffer buffer) {
        var progBytes = PROG_NAME.getBytes(StandardCharsets.UTF_16LE);
        var length = 1 + 4 + (1 + progBytes.length) + 4;
        buffer.write(TdsTokenType.LOGINACK);
        buffer.writeUShortLE(length);
        buffer.write((byte) 1); //SQL_DFLT interface
        buffer.write(TDS_VERSION);
        buffer.writeBVarchar(PROG_NAME);
        buffer.write(PROG_VERSION);
    }
}
