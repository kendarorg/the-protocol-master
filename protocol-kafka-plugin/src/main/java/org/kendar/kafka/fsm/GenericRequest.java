package org.kendar.kafka.fsm;

/** Catch-all request state (matches any api key). Must be LAST in the switch-case. */
public class GenericRequest extends KafkaRequestState {

    public GenericRequest(Class<?>... events) {
        super(events);
    }

    @Override
    protected boolean matches(short apiKey) {
        return true;
    }

    @Override
    protected KafkaResponseState responseFor(int correlationId, short apiVersion) {
        return new GenericResponse(correlationId, apiVersion);
    }
}
