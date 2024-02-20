package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;

public class AuthenticationOk implements NetworkReturnMessage {
    @Override
    public void write(BBuffer resultBuffer) {
        resultBuffer.write((byte) 'R');
        resultBuffer.writeInt(8);
        resultBuffer.writeInt(0);
    }
}
