package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.ReturnMessage;

public class NoticeReponse extends ReturnMessage {
    private final int pid;

    public NoticeReponse(int pid) {

        this.pid = pid;
    }

    @Override
    public void write(BBuffer resultBuffer) {

        resultBuffer.write((byte) 'N');
        //resultBuffer.writeInt(4);
//        resultBuffer.writeInt(pid);
//        resultBuffer.write((byte)0);
//        resultBuffer.write((byte)0);
    }
}
