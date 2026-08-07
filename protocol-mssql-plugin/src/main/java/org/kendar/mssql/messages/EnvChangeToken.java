package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.TdsTokenType;

import java.nio.charset.StandardCharsets;

/**
 * ENVCHANGE with string old/new values (database, language, packet size)
 */
public class EnvChangeToken extends TdsToken {
    private final byte type;
    private final String newValue;
    private final String oldValue;

    public EnvChangeToken(byte type, String newValue, String oldValue) {
        this.type = type;
        this.newValue = newValue == null ? "" : newValue;
        this.oldValue = oldValue == null ? "" : oldValue;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        var newBytes = newValue.getBytes(StandardCharsets.UTF_16LE);
        var oldBytes = oldValue.getBytes(StandardCharsets.UTF_16LE);
        var length = 1 + (1 + newBytes.length) + (1 + oldBytes.length);
        buffer.write(TdsTokenType.ENVCHANGE);
        buffer.writeUShortLE(length);
        buffer.write(type);
        buffer.writeBVarchar(newValue);
        buffer.writeBVarchar(oldValue);
    }
}
