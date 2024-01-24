package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.ReturnMessage;

public class CloseComplete extends ReturnMessage {
    @Override
    public void write(BBuffer resultBuffer) {
        resultBuffer.write((byte) '3');
        resultBuffer.writeInt(4);
    }
}
