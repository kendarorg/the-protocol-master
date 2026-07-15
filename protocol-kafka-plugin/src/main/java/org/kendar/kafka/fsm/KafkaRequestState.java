package org.kendar.kafka.fsm;

import org.kendar.kafka.KafkaProxy;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.fsm.events.KafkaRequestEvent;
import org.kendar.kafka.messages.KafkaRawMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.ProxyConnection;

import java.util.Iterator;

/**
 * Base client-request state. Matches on {@code apiKey}, forwards the whole raw
 * request frame to the broker and expects a correlation-id-matched response
 * state (byte-exact passthrough by default; semantic subclasses pair a rewriting
 * / capping {@link KafkaResponseState}). The blocking {@code sendAndExpect}
 * serializes per connection but correlation-id matching keeps it pipelining-safe
 * (protocol-kafka.md §4 / §6.3).
 */
public abstract class KafkaRequestState extends ProtoState {

    protected KafkaRequestState(Class<?>... events) {
        super(events);
    }

    /** Whether this state handles the given api key. */
    protected abstract boolean matches(short apiKey);

    /** The response state to expect for this request (paired semantic/generic). */
    protected abstract KafkaResponseState responseFor(int correlationId, short apiVersion);

    public boolean canRun(KafkaRequestEvent event) {
        return matches(event.getApiKey());
    }

    public Iterator<ProtoStep> execute(KafkaRequestEvent event) {
        var context = (KafkaContext) event.getContext();
        var proxy = (KafkaProxy) context.getProxy();
        var connection = (ProxyConnection) context.getValue("CONNECTION");

        var raw = event.getBuffer().getAll();
        var request = new KafkaRawMessage(raw);
        var response = responseFor(event.getCorrelationId(), event.getApiVersion());

        return iteratorOfRunnable(() -> proxy.sendAndExpect(context, connection, request, response));
    }
}
