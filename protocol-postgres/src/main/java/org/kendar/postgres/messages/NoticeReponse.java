package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;

public class NoticeReponse implements NetworkReturnMessage {
    private byte data = 'N';

    public NoticeReponse(byte data) {
        this.data = data;
    }
    public NoticeReponse() {

    }

    @Override
    public void write(BBuffer resultBuffer) {

        resultBuffer.write( data);
    }
}
