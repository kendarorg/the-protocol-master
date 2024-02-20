package org.kendar.proxy;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.storage.StorageRoot;

public abstract class Proxy<T extends StorageRoot> {

    public NetworkProtoDescriptor protocol;
    protected T storage;

    public NetworkProtoDescriptor getProtocol() {
        return protocol;
    }

    public void setProtocol(NetworkProtoDescriptor protocol) {
        this.protocol = protocol;
    }

    public abstract ProxyConnection connect(NetworkProtoContext context);

    public abstract void initialize();

    public T getStorage() {
        return storage;
    }

    public void setStorage(T storage) {
        this.storage = storage;
        this.storage.initialize();
    }
}
