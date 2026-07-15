package org.kendar.kafka.messages;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;

/**
 * A verbatim Kafka frame used purely as an outbound message: it writes its
 * stored raw bytes unchanged. Used to forward a client request to the broker
 * (byte-exact passthrough).
 */
public class KafkaRawMessage implements NetworkReturnMessage {
    private final byte[] raw;

    public KafkaRawMessage(byte[] raw) {
        this.raw = raw;
    }

    public byte[] getRaw() {
        return raw;
    }

    @Override
    public void write(BBuffer rb) {
        rb.write(raw);
    }
}
