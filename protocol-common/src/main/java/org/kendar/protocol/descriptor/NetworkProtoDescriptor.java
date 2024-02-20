package org.kendar.protocol.descriptor;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.Proxy;
import org.kendar.server.ClientServerChannel;

public abstract class NetworkProtoDescriptor extends ProtoDescriptor {

    private Proxy proxyInstance;
    private boolean proxy;

    @Override
    public void initialize() {
        if (isProxy()) {
            proxyInstance.initialize();
        }
        super.initialize();
    }

    public boolean sendImmediateGreeting() {
        return false;
    }

    public abstract boolean isBe();

    public abstract int getPort();

    public boolean isProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxyInstance) {
        this.proxyInstance = proxyInstance;
        this.proxyInstance.setProtocol(this);
        this.proxy = true;
    }

    public ProtoContext buildContext(ClientServerChannel client) {
        var context = (NetworkProtoContext) createContext(this);
        context.setClient(client);
        if (isProxy()) {
            var conn = proxyInstance.connect(context);
            context.setValue("CONNECTION", conn);
            context.setProxy(proxyInstance);
        }
        return context;
    }

    public BBuffer buildBuffer() {
        return new BBuffer(isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }
}
