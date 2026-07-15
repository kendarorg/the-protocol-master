package org.kendar.kafka.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka API keys we reference by name (for tagging) plus, for the APIs we decode
 * semantically, the version at which their request/response switch to the
 * flexible (KIP-482 tagged-field / compact) encoding.
 * <p>
 * Only a handful of keys are modelled; every other key is handled generically
 * (raw passthrough), so a full 1:1 model of all ~70 keys is unnecessary — but a
 * name lookup is provided for all common keys so recordings carry a readable
 * {@code api} tag (protocol-kafka.md §3.3).
 */
public final class KafkaApiKeys {
    public static final short PRODUCE = 0;
    public static final short FETCH = 1;
    public static final short LIST_OFFSETS = 2;
    public static final short METADATA = 3;
    public static final short OFFSET_COMMIT = 8;
    public static final short OFFSET_FETCH = 9;
    public static final short FIND_COORDINATOR = 10;
    public static final short JOIN_GROUP = 11;
    public static final short HEARTBEAT = 12;
    public static final short LEAVE_GROUP = 13;
    public static final short SYNC_GROUP = 14;
    public static final short DESCRIBE_GROUPS = 15;
    public static final short LIST_GROUPS = 16;
    public static final short SASL_HANDSHAKE = 17;
    public static final short API_VERSIONS = 18;
    public static final short CREATE_TOPICS = 19;
    public static final short DELETE_TOPICS = 20;
    public static final short INIT_PRODUCER_ID = 22;
    public static final short SASL_AUTHENTICATE = 36;
    public static final short DESCRIBE_CLUSTER = 60;

    private static final Map<Short, String> NAMES = new HashMap<>();
    // Version at which the request/response of a given key becomes flexible.
    private static final Map<Short, Short> FLEXIBLE_FROM = new HashMap<>();

    static {
        NAMES.put(PRODUCE, "Produce");
        NAMES.put(FETCH, "Fetch");
        NAMES.put(LIST_OFFSETS, "ListOffsets");
        NAMES.put(METADATA, "Metadata");
        NAMES.put((short) 4, "LeaderAndIsr");
        NAMES.put((short) 5, "StopReplica");
        NAMES.put((short) 6, "UpdateMetadata");
        NAMES.put((short) 7, "ControlledShutdown");
        NAMES.put(OFFSET_COMMIT, "OffsetCommit");
        NAMES.put(OFFSET_FETCH, "OffsetFetch");
        NAMES.put(FIND_COORDINATOR, "FindCoordinator");
        NAMES.put(JOIN_GROUP, "JoinGroup");
        NAMES.put(HEARTBEAT, "Heartbeat");
        NAMES.put(LEAVE_GROUP, "LeaveGroup");
        NAMES.put(SYNC_GROUP, "SyncGroup");
        NAMES.put(DESCRIBE_GROUPS, "DescribeGroups");
        NAMES.put(LIST_GROUPS, "ListGroups");
        NAMES.put(SASL_HANDSHAKE, "SaslHandshake");
        NAMES.put(API_VERSIONS, "ApiVersions");
        NAMES.put(CREATE_TOPICS, "CreateTopics");
        NAMES.put(DELETE_TOPICS, "DeleteTopics");
        NAMES.put((short) 21, "DeleteRecords");
        NAMES.put(INIT_PRODUCER_ID, "InitProducerId");
        NAMES.put((short) 23, "OffsetForLeaderEpoch");
        NAMES.put((short) 24, "AddPartitionsToTxn");
        NAMES.put((short) 25, "AddOffsetsToTxn");
        NAMES.put((short) 26, "EndTxn");
        NAMES.put((short) 32, "DescribeConfigs");
        NAMES.put(SASL_AUTHENTICATE, "SaslAuthenticate");
        NAMES.put(DESCRIBE_CLUSTER, "DescribeCluster");

        FLEXIBLE_FROM.put(PRODUCE, (short) 9);
        FLEXIBLE_FROM.put(FETCH, (short) 12);
        FLEXIBLE_FROM.put(METADATA, (short) 9);
        FLEXIBLE_FROM.put(FIND_COORDINATOR, (short) 3);
        FLEXIBLE_FROM.put(API_VERSIONS, (short) 3);
        FLEXIBLE_FROM.put(DESCRIBE_CLUSTER, (short) 0);
        FLEXIBLE_FROM.put(OFFSET_FETCH, (short) 6);
        FLEXIBLE_FROM.put(JOIN_GROUP, (short) 6);
        FLEXIBLE_FROM.put(SYNC_GROUP, (short) 4);
        FLEXIBLE_FROM.put(HEARTBEAT, (short) 4);
        FLEXIBLE_FROM.put(OFFSET_COMMIT, (short) 8);
    }

    private KafkaApiKeys() {
    }

    public static String nameOf(short apiKey) {
        return NAMES.getOrDefault(apiKey, "Api" + apiKey);
    }

    /** Whether the request/response body of {@code apiKey} at {@code apiVersion} is flexible. */
    public static boolean isFlexible(short apiKey, short apiVersion) {
        Short from = FLEXIBLE_FROM.get(apiKey);
        return from != null && apiVersion >= from;
    }
}
