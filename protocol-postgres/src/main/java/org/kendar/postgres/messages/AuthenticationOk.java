package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;

public class AuthenticationOk implements NetworkReturnMessage {
    private final int authValue;

    public AuthenticationOk(int authValue) {
        this.authValue = authValue;
    }

    public AuthenticationOk(){
        this.authValue = 0;
    }
    @Override
    public void write(BBuffer resultBuffer) {
        resultBuffer.write((byte) 'R');
        resultBuffer.writeInt(8);
        resultBuffer.writeInt(0);
    }
}
