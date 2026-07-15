package org.kendar.kafka.fsm;

import org.kendar.kafka.enums.KafkaApiKeys;

/** Forwards Metadata (3); the response has broker addresses rewritten (§3.1). */
public class MetadataRequest extends KafkaRequestState {

    public MetadataRequest(Class<?>... events) {
        super(events);
    }

    @Override
    protected boolean matches(short apiKey) {
        return apiKey == KafkaApiKeys.METADATA;
    }

    @Override
    protected KafkaResponseState responseFor(int correlationId, short apiVersion) {
        return new MetadataResponse(correlationId, apiVersion);
    }
}
