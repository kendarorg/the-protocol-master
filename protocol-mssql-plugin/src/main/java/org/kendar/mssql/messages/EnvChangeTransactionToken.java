package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.EnvChangeType;
import org.kendar.mssql.constants.TdsTokenType;

/**
 * ENVCHANGE with binary old/new values (begin/commit/rollback transaction);
 * the values are transaction descriptors
 */
public class EnvChangeTransactionToken extends TdsToken {
    private final byte type;
    private final long newDescriptor;
    private final long oldDescriptor;

    public EnvChangeTransactionToken(byte type, long newDescriptor, long oldDescriptor) {
        this.type = type;
        this.newDescriptor = newDescriptor;
        this.oldDescriptor = oldDescriptor;
    }

    @Override
    public void write(MssqlBBuffer buffer) {
        var newLen = type == EnvChangeType.BEGIN_TRANSACTION ? 8 : 0;
        var oldLen = type == EnvChangeType.BEGIN_TRANSACTION ? 0 : 8;
        var length = 1 + (1 + newLen) + (1 + oldLen);
        buffer.write(TdsTokenType.ENVCHANGE);
        buffer.writeUShortLE(length);
        buffer.write(type);
        buffer.write((byte) newLen);
        if (newLen == 8) {
            buffer.writeULongLE(newDescriptor);
        }
        buffer.write((byte) oldLen);
        if (oldLen == 8) {
            buffer.writeULongLE(oldDescriptor);
        }
    }
}
