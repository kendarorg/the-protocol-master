package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.ReturnMessage;

public class BindComplete implements ReturnMessage {
    @Override
    public void write(BBuffer resultBuffer) {
        resultBuffer.write((byte) '2');
        resultBuffer.writeInt(4);
    }
}
