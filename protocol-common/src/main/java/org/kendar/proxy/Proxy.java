package org.kendar.proxy;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.storage.Storage;

/**
 * Base proxy implementation
 *
 * @param <T>
 */
public abstract class Proxy<T extends Storage> {

    /**
     * Descriptor (of course network like)
     */
    public NetworkProtoDescriptor protocol;

    /**
     * (Eventual) storage
     */
    protected T storage;

    /**
     * Retrieve the protocol data
     *
     * @return
     */
    public NetworkProtoDescriptor getProtocol() {
        return protocol;
    }

    /**
     * Set the protocol data
     *
     * @param protocol
     */
    public void setProtocol(NetworkProtoDescriptor protocol) {
        this.protocol = protocol;
    }

    /**
     * Implementation specific when connecting to a real server
     *
     * @param context
     * @return
     */
    public abstract ProxyConnection connect(NetworkProtoContext context);

    /**
     * Initialize the proxy
     */
    public abstract void initialize();

    /**
     * Get the storage
     *
     * @return
     */
    public T getStorage() {
        return storage;
    }

    /**
     * Set and initialize the storage
     *
     * @param storage
     */
    public void setStorage(T storage) {
        this.storage = storage;
        this.storage.initialize();
    }
}
