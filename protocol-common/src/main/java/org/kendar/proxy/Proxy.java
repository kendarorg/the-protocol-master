package org.kendar.proxy;

import org.kendar.protocol.ProtoDescriptor;
import org.kendar.storage.StorageRoot;

public abstract class Proxy<T extends StorageRoot> {

    protected T storage;

    public ProtoDescriptor getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtoDescriptor protocol) {
        this.protocol = protocol;
    }

    public ProtoDescriptor protocol;

    public abstract ProxyConnection connect();

    public abstract void initialize();

    public T getStorage() {
        return storage;
    }

    public void setStorage(T storage) {
        this.storage = storage;
        this.storage.initialize();
    }
}
