package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsTokenType;

import java.nio.charset.StandardCharsets;

public class ErrorToken extends TdsToken {
    private final int number;
    private final String message;

    public ErrorToken(String message) {
        this(50000, message);
    }

    public ErrorToken(int number, String message) {
        this.number = number;
        this.message = message == null ? "MISSING MESSAGE" : message;
    }

    protected byte getTokenType() {
        return TdsTokenType.ERROR;
    }

    protected byte getSeverity() {
        return 16;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        var serverName = "TPM";
        var msgBytes = message.getBytes(StandardCharsets.UTF_16LE);
        var serverBytes = serverName.getBytes(StandardCharsets.UTF_16LE);
        var length = 4 + 1 + 1 + (2 + msgBytes.length) + (1 + serverBytes.length) + 1 + 4;
        buffer.write(getTokenType());
        buffer.writeUShortLE(length);
        buffer.writeUIntLE(number);
        buffer.write((byte) 1); //state
        buffer.write(getSeverity()); //class
        buffer.writeUsVarchar(message);
        buffer.writeBVarchar(serverName);
        buffer.writeBVarchar("");
        buffer.writeUIntLE(1); //line number
    }
}
