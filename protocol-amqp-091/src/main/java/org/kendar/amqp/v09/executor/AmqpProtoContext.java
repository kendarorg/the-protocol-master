package org.kendar.amqp.v09.executor;

import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoDescriptor;
import org.kendar.server.Channel;

public class AmqpProtoContext extends ProtoContext {
    public AmqpProtoContext(ProtoDescriptor descriptor, Channel client) {
        super(descriptor, client);
    }

    private short channel=1;

    public short getChannel(){
        return ++channel;
    }
}
