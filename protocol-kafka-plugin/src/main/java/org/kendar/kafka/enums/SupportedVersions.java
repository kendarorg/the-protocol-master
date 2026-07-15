package org.kendar.kafka.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Version-capping table (protocol-kafka.md §3.2). We intercept the ApiVersions
 * (18) response and cap each key's advertised max version to {@code min(brokerMax,
 * ourMax)}, so clients never negotiate a semantic-API version our codec cannot
 * decode. Keys absent from this table keep the broker's advertised range (they
 * are pure passthrough — only the header layout matters, and that is known).
 * <ul>
 *   <li>Metadata &le; 12 (v13+ moves to topic UUIDs)</li>
 *   <li>Fetch &le; 12 (same reason)</li>
 *   <li>Produce &le; 9</li>
 *   <li>FindCoordinator &le; 4</li>
 * </ul>
 */
public final class SupportedVersions {
    private static final Map<Short, Short> MAX = new HashMap<>();

    static {
        MAX.put(KafkaApiKeys.METADATA, (short) 12);
        MAX.put(KafkaApiKeys.FETCH, (short) 12);
        MAX.put(KafkaApiKeys.PRODUCE, (short) 9);
        MAX.put(KafkaApiKeys.FIND_COORDINATOR, (short) 4);
    }

    private SupportedVersions() {
    }

    /** Our max supported version for a key, or {@code null} if uncapped (passthrough). */
    public static Short ourMax(short apiKey) {
        return MAX.get(apiKey);
    }

    /** Caps {@code brokerMax} to our supported max for the key. */
    public static short cap(short apiKey, short brokerMax) {
        Short ours = MAX.get(apiKey);
        if (ours == null) {
            return brokerMax;
        }
        return (short) Math.min(brokerMax, ours);
    }
}
