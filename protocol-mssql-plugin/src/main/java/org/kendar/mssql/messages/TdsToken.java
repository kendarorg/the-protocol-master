package org.kendar.mssql.messages;

import org.kendar.mssql.buffers.MssqlBBuffer;

/**
 * A single token of a TDS token stream response
 */
public abstract class TdsToken {
    public abstract void write(MssqlBBuffer buffer);
}
