package org.kendar.amqp.v09.executor;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;

public class AmqpProtoContext extends NetworkProtoContext {
    private short channel = 1;

    public AmqpProtoContext(ProtoDescriptor descriptor) {
        super(descriptor);
    }

    public short getChannel() {
        return ++channel;
    }
}
