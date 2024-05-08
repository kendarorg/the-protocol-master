package org.kendar.proto.silly;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.server.ClientServerChannel;

import java.util.ArrayList;
import java.util.List;

public class SillyContext extends ProtoContext {
    private List<ReturnMessage> result = new ArrayList<>();

    public SillyContext(ProtoDescriptor descriptor, ClientServerChannel client) {
        super(descriptor);
    }

    public SillyContext(SillyProtocol descriptor) {
        super(descriptor);
    }

    public List<ReturnMessage> getResult() {
        return result;
    }

    @Override
    public void disconnect(Object connection) {

    }

    @Override
    public void write(ReturnMessage returnMessage) {
        result.add(returnMessage);
    }
}
