package org.kendar.proto.taggedfsm;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.server.ClientServerChannel;

import java.util.ArrayList;
import java.util.List;

public class TaggedContext extends ProtoContext {
    private List<ReturnMessage> result = new ArrayList<>();

    public TaggedContext(ProtoDescriptor descriptor, ClientServerChannel client,
                         int contextId) {
        super(descriptor, contextId);
    }

    public TaggedContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    @Override
    public void disconnect(Object connection) {

    }

    public List<ReturnMessage> getResult() {
        return result;
    }

    @Override
    public void write(ReturnMessage returnMessage) {
        result.add(returnMessage);
    }
}
