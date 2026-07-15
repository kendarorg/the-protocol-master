package org.kendar.kafka.fsm;

import org.kendar.kafka.enums.KafkaApiKeys;

/** Forwards FindCoordinator (10); the response has the coordinator address rewritten (§3.1). */
public class FindCoordinatorRequest extends KafkaRequestState {

    public FindCoordinatorRequest(Class<?>... events) {
        super(events);
    }

    @Override
    protected boolean matches(short apiKey) {
        return apiKey == KafkaApiKeys.FIND_COORDINATOR;
    }

    @Override
    protected KafkaResponseState responseFor(int correlationId, short apiVersion) {
        return new FindCoordinatorResponse(correlationId, apiVersion);
    }
}
