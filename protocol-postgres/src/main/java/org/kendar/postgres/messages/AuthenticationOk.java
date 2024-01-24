package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.ReturnMessage;

public class AuthenticationOk extends ReturnMessage {
    @Override
    public void write(BBuffer resultBuffer) {
        resultBuffer.write((byte) 'R');
        resultBuffer.writeInt(8);
        resultBuffer.writeInt(0);
    }
}
