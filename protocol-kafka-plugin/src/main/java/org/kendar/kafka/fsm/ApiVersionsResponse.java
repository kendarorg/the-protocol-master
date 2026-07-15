package org.kendar.kafka.fsm;

import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.enums.KafkaApiKeys;
import org.kendar.kafka.enums.SupportedVersions;
import org.kendar.kafka.utils.KafkaBBuffer;

/**
 * Caps the max advertised version of each API in the ApiVersions (18) response
 * to {@code min(brokerMax, ourMax)} so clients never negotiate a version our
 * codec cannot decode (protocol-kafka.md §3.2).
 * <p>
 * The cap only lowers the max_version int16 in place — count, api_key, min and
 * the frame length are all unchanged — so we edit the bytes directly instead of
 * re-encoding. NOTE: the ApiVersions response header is <b>always v0</b> (no
 * tagged fields), even when the request was flexible (v3+); only the body's
 * api_keys array is flexible.
 */
public class ApiVersionsResponse extends KafkaResponseState {

    public ApiVersionsResponse(int expectedCorrelationId, short apiVersion) {
        super(expectedCorrelationId, apiVersion);
    }

    @Override
    protected KafkaResponseState newInstance() {
        return new ApiVersionsResponse(expectedCorrelationId, apiVersion);
    }

    @Override
    protected byte[] transform(byte[] raw, KafkaContext context) {
        boolean flexible = KafkaApiKeys.isFlexible(KafkaApiKeys.API_VERSIONS, apiVersion);
        var kb = new KafkaBBuffer(raw);
        kb.getInt();                 // size
        kb.getInt();                 // correlation id (response header always v0)
        kb.getShort();               // error_code
        int count = kb.readArrayCount(flexible);
        for (int i = 0; i < count; i++) {
            short apiKey = kb.getShort();   // api_key
            kb.getShort();                  // min_version
            int maxPos = kb.getPosition();
            short max = kb.getShort();      // max_version
            short capped = SupportedVersions.cap(apiKey, max);
            if (capped != max) {
                kb.writeShort(capped, maxPos);
            }
            if (flexible) {
                kb.readTaggedFieldsRaw();
            }
        }
        return kb.getAll();
    }
}
