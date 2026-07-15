package org.kendar.kafka.fsm;

import org.kendar.kafka.KafkaProtocol;
import org.kendar.kafka.context.KafkaContext;
import org.kendar.kafka.utils.KafkaBBuffer;

/**
 * Rewrites broker {@code host:port} in a DescribeCluster (60) response (always
 * flexible). Best-effort for v0/v1; on any decode surprise the base class falls
 * back to byte-exact passthrough.
 */
public class DescribeClusterResponse extends KafkaResponseState {

    public DescribeClusterResponse(int expectedCorrelationId, short apiVersion) {
        super(expectedCorrelationId, apiVersion);
    }

    @Override
    protected KafkaResponseState newInstance() {
        return new DescribeClusterResponse(expectedCorrelationId, apiVersion);
    }

    @Override
    protected byte[] transform(byte[] raw, KafkaContext context) {
        var proto = (KafkaProtocol) context.getDescriptor();
        var host = proto.getAdvertisedHost();
        var port = proto.getPort();

        var in = new KafkaBBuffer(raw);
        in.getInt();                                   // size
        int correlationId = in.getInt();
        byte[] headerTagged = in.readTaggedFieldsRaw();

        var body = new KafkaBBuffer();
        body.writeInt(correlationId);
        body.write(headerTagged);

        body.writeInt(in.getInt());                    // throttle_time_ms
        body.writeShort(in.getShort());                // error_code
        body.writeCompactString(in.readCompactString()); // error_message
        if (apiVersion >= 1) {
            body.write(in.get());                      // endpoint_type
        }
        body.writeCompactString(in.readCompactString()); // cluster_id
        body.writeInt(in.getInt());                    // controller_id

        int count = in.readArrayCount(true);
        body.writeArrayCount(count, true);
        for (int i = 0; i < count; i++) {
            int brokerId = in.getInt();
            in.readCompactString();                    // host (discarded)
            in.getInt();                               // port (discarded)
            String rack = in.readCompactString();
            byte[] tagged = in.readTaggedFieldsRaw();

            body.writeInt(brokerId);
            body.writeCompactString(host);
            body.writeInt(port);
            body.writeCompactString(rack);
            body.write(tagged);
        }
        body.write(in.peekRemaining());                // cluster_authorized_operations, tagged

        var out = new KafkaBBuffer();
        out.writeInt(body.size());
        out.write(body.getAll());
        return out.getAll();
    }
}
