package org.kendar.postgres.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;

import java.nio.charset.StandardCharsets;

public class ErrorResponse implements NetworkReturnMessage {
    private String message;

    public ErrorResponse(String message) {

        this.message = message;
    }

    @Override
    public void write(BBuffer buffer) {
        if (message == null) {
            message = "MISSING MESSAGE";
        }
        var s = "FATAL".getBytes(StandardCharsets.UTF_8);
        var m = message.getBytes(StandardCharsets.UTF_8);

        buffer.write((byte) 'E');
        buffer.writeInt(4 + s.length + m.length + 2 + 2);
        buffer.write((byte) 'S'); // severity
        buffer.write(s);
        buffer.write((byte) 0);
        buffer.write((byte) 'M'); // type
        buffer.write(m);
        buffer.write((byte) 0);
    }
}
