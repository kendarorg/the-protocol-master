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
        //Force system_error  https://www.postgresql.org/docs/9.3/errcodes-appendix.html
        var ec = "58000".getBytes(StandardCharsets.UTF_8);

        buffer.write((byte) 'E');
        buffer.writeInt(4 + s.length + 2 + m.length + 2 + ec.length + 2);
        buffer.write((byte) 'S'); // severity
        buffer.write(s);
        buffer.write((byte) 0);
        buffer.write((byte) 'M'); // type
        buffer.write(m);
        buffer.write((byte) 0);
        buffer.write((byte) 'C'); // errorCode
        buffer.write(ec);
        buffer.write((byte) 0);
    }
}
