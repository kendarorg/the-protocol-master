package org.kendar.kafka.fsm;

import org.kendar.kafka.enums.KafkaApiKeys;

/** Forwards DescribeCluster (60); the response has broker addresses rewritten (§3.1). */
public class DescribeClusterRequest extends KafkaRequestState {

    public DescribeClusterRequest(Class<?>... events) {
        super(events);
    }

    @Override
    protected boolean matches(short apiKey) {
        return apiKey == KafkaApiKeys.DESCRIBE_CLUSTER;
    }

    @Override
    protected KafkaResponseState responseFor(int correlationId, short apiVersion) {
        return new DescribeClusterResponse(correlationId, apiVersion);
    }
}
