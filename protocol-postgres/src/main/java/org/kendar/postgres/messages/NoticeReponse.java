package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;

public class NoticeReponse implements NetworkReturnMessage {

    private final int pid;

    public NoticeReponse(int pid) {

        this.pid = pid;
    }

    @Override
    public void write(BBuffer resultBuffer) {

        resultBuffer.write((byte) 'N');
    }
}
