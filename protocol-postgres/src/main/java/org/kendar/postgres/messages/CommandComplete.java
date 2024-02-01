package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.ReturnMessage;

import java.nio.charset.StandardCharsets;

public class CommandComplete implements ReturnMessage {

    private final String tag;

    public CommandComplete(String tag) {

        this.tag = tag;
    }

    @Override
    public void write(BBuffer buffer) {
        var length = 4 + tag.length() + 1;
        buffer.write((byte) 'C');
        buffer.writeInt(length);
        buffer.write(tag.getBytes(StandardCharsets.US_ASCII));
        buffer.write((byte) 0);
    }
}
