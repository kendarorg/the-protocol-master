package org.kendar.redis.fsm.events;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.redis.parser.Resp3Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class Resp3Message extends ProtocolEvent implements NetworkReturnMessage {
    private static final Resp3Parser parser = new Resp3Parser();
    private static final Logger log = LoggerFactory.getLogger(Resp3Message.class);
    private final Object data;
    private final String message;

    public Resp3Message(ProtoContext context, Class<?> prevState, JsonNode data) {
        super(context, prevState);
        try {
            this.message = parser.serialize(data);
            this.data = parser.parse(message);
        } catch (Exception ex) {
            throw new TPMProtocolException("UNABLE TO DESERIALIZE FROM JSON NODE");
        }
    }

    public Resp3Message(ProtoContext context, Class<?> prevState, Object data, String message) {
        super(context, prevState);
        this.data = data;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        try {
            var data = message.getBytes(StandardCharsets.US_ASCII);
            resultBuffer.write(data);
        } catch (Exception ex) {
            log.error("Error writing message", ex);
        }
    }

    @Override
    public String toString() {
        return "Resp3Message{" +
                "message='" + message + '\'' +
                '}';
    }
}

