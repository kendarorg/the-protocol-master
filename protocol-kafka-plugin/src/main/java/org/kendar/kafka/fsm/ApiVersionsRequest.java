package org.kendar.kafka.fsm;

import org.kendar.kafka.enums.KafkaApiKeys;

/** Forwards ApiVersions (18); the response is version-capped (§3.2). */
public class ApiVersionsRequest extends KafkaRequestState {

    public ApiVersionsRequest(Class<?>... events) {
        super(events);
    }

    @Override
    protected boolean matches(short apiKey) {
        return apiKey == KafkaApiKeys.API_VERSIONS;
    }

    @Override
    protected KafkaResponseState responseFor(int correlationId, short apiVersion) {
        return new ApiVersionsResponse(correlationId, apiVersion);
    }
}
