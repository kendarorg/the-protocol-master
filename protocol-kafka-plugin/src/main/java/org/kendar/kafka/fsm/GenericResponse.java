package org.kendar.kafka.fsm;

/**
 * Byte-exact passthrough response: relays the broker frame to the client
 * unchanged. Used for every API that needs no rewrite.
 */
public class GenericResponse extends KafkaResponseState {

    public GenericResponse(int expectedCorrelationId, short apiVersion) {
        super(expectedCorrelationId, apiVersion);
    }

    @Override
    protected KafkaResponseState newInstance() {
        return new GenericResponse(expectedCorrelationId, apiVersion);
    }
}
