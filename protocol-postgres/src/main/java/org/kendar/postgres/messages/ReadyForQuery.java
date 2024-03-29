package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;

public class ReadyForQuery implements NetworkReturnMessage {
    private final boolean inTransaction;

    public ReadyForQuery(boolean inTransaction) {

        this.inTransaction = inTransaction;
    }

    @Override
    public void write(BBuffer buffer) {
        buffer.write((byte) 'Z');
        buffer.writeInt(5);
        buffer.write((byte) (inTransaction ? 'T' : 'I'));
    }
}
