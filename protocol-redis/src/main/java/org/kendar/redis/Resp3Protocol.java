package org.kendar.redis;

import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;

public class Resp3Protocol extends NetworkProtoDescriptor {
    private static final int PORT = 6379;
    private  int port = PORT;

    private Resp3Protocol() {}
    public Resp3Protocol(int port) {
        this.port = port;
    }


    @Override
    public boolean isBe() {
        return true;
    }

    @Override
    public int getPort() {
        return 6379;
    }

    @Override
    protected void initializeProtocol() {
        //addInterruptState(new Resp3FrameTranslator(BytesEvent.class));

    }

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        return new Reps3Context(protoDescriptor);
    }
}
