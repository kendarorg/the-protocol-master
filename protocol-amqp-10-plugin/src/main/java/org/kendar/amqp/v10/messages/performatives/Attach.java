package org.kendar.amqp.v10.messages.performatives;

import org.kendar.amqp.v10.codec.Amqp10Frames;
import org.kendar.amqp.v10.codec.DescribedType;
import org.kendar.amqp.v10.context.Amqp10ProtoContext;
import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.Amqp10BaseFrame;
import org.kendar.amqp.v10.utils.ReceiverLink;
import org.kendar.protocol.context.NetworkProtoContext;

import java.util.List;

/** AMQP 1.0 {@code attach} performative (link setup, descriptor 0x12). */
public class Attach extends Amqp10BaseFrame {
    public Attach() {
        super();
    }

    public Attach(Class<?>... events) {
        super(events);
    }

    @Override
    protected long getDescriptorCode() {
        return Performatives.ATTACH;
    }

    /**
     * When the broker attaches as a {@code sender} (role field == false) in
     * response to a client consumer, record the (channel, handle, source) so the
     * publish plugin can inject transfers onto exactly this delivery link. This is
     * the AMQP 1.0 analog of v09's {@code BASIC_CONSUME_CH_} bookkeeping.
     */
    @Override
    protected void capture(NetworkProtoContext context, byte[] raw, boolean proxyed) {
        if (!proxyed || !(context instanceof Amqp10ProtoContext)) {
            return; // only the broker's attach response identifies a delivery link
        }
        var fields = Amqp10Frames.fields(Amqp10Frames.performative(raw));
        if (fields.isEmpty()) {
            return;
        }
        var name = Amqp10Frames.field(fields, 0);
        var role = Amqp10Frames.field(fields, 2); // false/null = sender, true = receiver
        if (Boolean.TRUE.equals(role)) {
            return; // broker is a receiver → this is a producer link, not a consumer
        }
        if (!(name instanceof String)) {
            return;
        }
        var handle = Amqp10Frames.asLong(Amqp10Frames.field(fields, 1));
        var link = new ReceiverLink((String) name, handle, channelOf(raw), sourceAddress(fields));
        ((Amqp10ProtoContext) context).putReceiverLink(link);
    }

    /** source (field 5) is a described {@code source}; its address is element 0. */
    private static String sourceAddress(List<?> fields) {
        var source = Amqp10Frames.field(fields, 5);
        if (source instanceof DescribedType && ((DescribedType) source).getValue() instanceof List) {
            var sfields = (List<?>) ((DescribedType) source).getValue();
            var addr = Amqp10Frames.field(sfields, 0);
            if (addr instanceof String) {
                return (String) addr;
            }
        }
        return null;
    }
}
