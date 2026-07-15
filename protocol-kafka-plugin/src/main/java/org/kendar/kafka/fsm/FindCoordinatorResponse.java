package org.kendar.kafka.fsm;

import org.kendar.kafka.KafkaProtocol;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.enums.KafkaApiKeys;
import org.kendar.kafka.utils.KafkaBBuffer;

/**
 * Rewrites the coordinator {@code host:port} in a FindCoordinator (10) response
 * to the proxy's own address (protocol-kafka.md §3.1). Handles the single-
 * coordinator form (v0–3) and the {@code coordinators[]} array form (v4+,
 * KIP-699). We cap FindCoordinator at v4.
 */
public class FindCoordinatorResponse extends KafkaResponseState {

    public FindCoordinatorResponse(int expectedCorrelationId, short apiVersion) {
        super(expectedCorrelationId, apiVersion);
    }

    @Override
    protected KafkaResponseState newInstance() {
        return new FindCoordinatorResponse(expectedCorrelationId, apiVersion);
    }

    @Override
    protected byte[] transform(byte[] raw, KafkaContext context) {
        boolean flexible = KafkaApiKeys.isFlexible(KafkaApiKeys.FIND_COORDINATOR, apiVersion);
        var proto = (KafkaProtocol) context.getDescriptor();
        var host = proto.getAdvertisedHost();
        var port = proto.getPort();

        var in = new KafkaBBuffer(raw);
        in.getInt();                               // size
        int correlationId = in.getInt();
        byte[] headerTagged = flexible ? in.readTaggedFieldsRaw() : null;

        var body = new KafkaBBuffer();
        body.writeInt(correlationId);
        if (flexible) {
            body.write(headerTagged);
        }

        if (apiVersion >= 1) {
            body.writeInt(in.getInt());            // throttle_time_ms
        }

        if (apiVersion >= 4) {
            int count = in.readArrayCount(flexible);
            body.writeArrayCount(count, flexible);
            for (int i = 0; i < count; i++) {
                String key = in.readString(flexible);
                in.getInt();                       // node_id (discarded)
                in.readString(flexible);           // host (discarded)
                in.getInt();                       // port (discarded)
                short errorCode = in.getShort();
                String errorMessage = in.readString(flexible);
                byte[] tagged = in.readTaggedFieldsRaw();

                body.writeString(key, flexible);
                body.writeInt(0);                  // node_id -> single proxy node
                body.writeString(host, flexible);
                body.writeInt(port);
                body.writeShort(errorCode);
                body.writeString(errorMessage, flexible);
                body.write(tagged);
            }
            body.write(in.readTaggedFieldsRaw());  // top-level tagged
        } else {
            short errorCode = in.getShort();
            String errorMessage = apiVersion >= 1 ? in.readString(flexible) : null;
            in.getInt();                           // node_id (discarded)
            in.readString(flexible);               // host (discarded)
            in.getInt();                           // port (discarded)

            body.writeShort(errorCode);
            if (apiVersion >= 1) {
                body.writeString(errorMessage, flexible);
            }
            body.writeInt(0);                      // node_id
            body.writeString(host, flexible);
            body.writeInt(port);
            if (flexible) {
                body.write(in.readTaggedFieldsRaw());
            }
        }

        var out = new KafkaBBuffer();
        out.writeInt(body.size());
        out.write(body.getAll());
        return out.getAll();
    }
}
