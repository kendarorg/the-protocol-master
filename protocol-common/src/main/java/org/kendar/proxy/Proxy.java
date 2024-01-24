package org.kendar.proxy;

import org.kendar.storage.StorageRoot;

public abstract class Proxy<T extends StorageRoot> {

    protected T storage;

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
