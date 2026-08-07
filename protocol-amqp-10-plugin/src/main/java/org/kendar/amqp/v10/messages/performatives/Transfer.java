package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.codec.Amqp10Frames;
import org.kendar.amqp.v10.context.Amqp10ProtoContext;
import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;
import org.kendar.protocol.context.NetworkProtoContext;

/**
 * AMQP 1.0 {@code transfer} performative (message delivery, descriptor 0x14).
 * <p>
 * Must remain a single repeatable state (never a sequence): fragments of one
 * delivery ({@code more=true}) may interleave with transfers on other links of
 * the same session. Fragment reassembly by handle + delivery-id lands in M3.
 */
public class Transfer extends Amqp10BaseFrame {
    public Transfer() {
        super();
    }

    public Transfer(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.TRANSFER;
    }

    /**
     * Keep the per-session delivery-id counter one past the highest delivery-id the
     * broker has sent, so a publish-plugin injected transfer picks a non-colliding
     * id. delivery-id is field 1 of the transfer performative.
     */
    @Override
    protected void capture(NetworkProtoContext context, byte[] raw, boolean proxyed) {
        if (!proxyed || !(context instanceof Amqp10ProtoContext)) {
            return;
        }
        var fields = Amqp10Frames.fields(Amqp10Frames.performative(raw));
        var deliveryId = Amqp10Frames.asLong(Amqp10Frames.field(fields, 1));
        if (deliveryId >= 0) {
            ((Amqp10ProtoContext) context).observeDeliveryId(channelOf(raw), deliveryId);
        }
    }
}
