package org.kendar.kafka.fsm;

import org.kendar.kafka.KafkaProtocol;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.enums.KafkaApiKeys;
import org.kendar.kafka.utils.KafkaBBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rewrites every broker {@code host:port} in a Metadata (3) response to the
 * proxy's own {@code advertisedHost} + bind port, so clients keep connecting to
 * the proxy instead of the real brokers (protocol-kafka.md §3.1). The brokers
 * array is early in the response, so we decode up to and including it, rewrite,
 * then copy the (large) remainder — topics etc. — verbatim.
 * <p>
 * Single-broker assumption (matches the framework's single connectionString);
 * a warning is logged if more than one broker is advertised.
 */
public class MetadataResponse extends KafkaResponseState {
    private static final Logger log = LoggerFactory.getLogger(MetadataResponse.class);

    public MetadataResponse(int expectedCorrelationId, short apiVersion) {
        super(expectedCorrelationId, apiVersion);
    }

    @Override
    protected KafkaResponseState newInstance() {
        return new MetadataResponse(expectedCorrelationId, apiVersion);
    }

    @Override
    protected byte[] transform(byte[] raw, KafkaContext context) {
        boolean flexible = KafkaApiKeys.isFlexible(KafkaApiKeys.METADATA, apiVersion);
        var proto = (KafkaProtocol) context.getDescriptor();
        var host = proto.getAdvertisedHost();
        var port = proto.getPort();

        var in = new KafkaBBuffer(raw);
        in.getInt();                                   // size
        int correlationId = in.getInt();
        byte[] headerTagged = flexible ? in.readTaggedFieldsRaw() : null;

        Integer throttle = apiVersion >= 3 ? in.getInt() : null;
        int brokerCount = in.readArrayCount(flexible);
        if (brokerCount > 1) {
            log.warn("Metadata advertised {} brokers; the proxy only models a single broker "
                    + "(protocol-kafka.md §3.1). Rewriting all to {}:{}", brokerCount, host, port);
        }

        var body = new KafkaBBuffer();
        body.writeInt(correlationId);
        if (flexible) {
            body.write(headerTagged);
        }
        if (throttle != null) {
            body.writeInt(throttle);
        }
        body.writeArrayCount(brokerCount, flexible);
        for (int i = 0; i < brokerCount; i++) {
            int nodeId = in.getInt();
            in.readString(flexible);                   // host (discarded)
            in.getInt();                               // port (discarded)
            String rack = apiVersion >= 1 ? in.readString(flexible) : null;
            byte[] brokerTagged = flexible ? in.readTaggedFieldsRaw() : null;

            body.writeInt(nodeId);
            body.writeString(host, flexible);
            body.writeInt(port);
            if (apiVersion >= 1) {
                body.writeString(rack, flexible);
            }
            if (flexible) {
                body.write(brokerTagged);
            }
        }
        body.write(in.peekRemaining());                // cluster_id, controller_id, topics, ...

        var out = new KafkaBBuffer();
        out.writeInt(body.size());
        out.write(body.getAll());
        return out.getAll();
    }
}
