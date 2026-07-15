package org.kendar.kafka.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.exceptions.TPMProtocolException;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.fsm.events.KafkaRequestEvent;
import org.kendar.kafka.utils.KafkaBBuffer;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Client-&gt;proxy frame splitter/decoder. Splits the inbound stream on the
 * Kafka 4-byte size prefix and parses just the request header
 * (api_key, api_version, correlation_id, client_id) — enough to route the
 * request and populate the in-flight map — while keeping the whole raw frame for
 * byte-exact forwarding. Registered as an interrupt state (evaluated ahead of
 * the normal FSM), analog of the v09/v10 {@code *FrameTranslator}.
 */
public class KafkaFrameTranslator extends ProtoState implements NetworkReturnMessage, InterruptProtoState {
    private static final Logger log = LoggerFactory.getLogger(KafkaFrameTranslator.class);

    public KafkaFrameTranslator() {
        super();
    }

    public KafkaFrameTranslator(Class<?>... events) {
        super(events);
    }

    @Override
    public void write(BBuffer rb) {
        throw new TPMProtocolException("Not implemented");
    }

    public boolean canRun(BytesEvent event) {
        var rb = event.getBuffer();
        rb.setPosition(0);
        if (rb.size() < 4) {
            return false;
        }
        var size = rb.getInt();
        rb.setPosition(0);
        if (size < 8) {
            // a valid request header is at least api_key(2)+api_version(2)+correlation_id(4)
            return false;
        }
        if (rb.size() < size + 4) {
            throw new AskMoreDataException();
        }
        return true;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var rb = event.getBuffer();
        rb.setPosition(0);
        var size = rb.getInt();
        rb.setPosition(0);
        var frame = rb.getBytes(size + 4);   // whole frame incl. the 4-byte size prefix

        var kb = new KafkaBBuffer(frame);
        kb.getInt();                      // size
        short apiKey = kb.getShort();
        short apiVersion = kb.getShort();
        int correlationId = kb.getInt();
        String clientId = null;
        try {
            clientId = kb.readString();   // header v1+ nullable client_id
        } catch (RuntimeException ignored) {
            // header v0 (no client_id) — extremely rare; leave null
        }

        var context = (KafkaContext) event.getContext();
        context.registerInFlight(correlationId, apiKey, apiVersion, clientId);

        var forward = new KafkaBBuffer(frame);
        log.debug("[CL>TP] Kafka request apiKey={} apiVersion={} correlationId={} clientId={}",
                apiKey, apiVersion, correlationId, clientId);
        context.send(new KafkaRequestEvent(context, event.getPrevState(), forward,
                apiKey, apiVersion, correlationId, clientId));
        return iteratorOfEmpty();
    }
}
