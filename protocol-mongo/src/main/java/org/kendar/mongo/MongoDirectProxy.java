package org.kendar.mongo;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;

public class MongoDirectProxy  extends Proxy {
    @Override
    public ProxyConnection connect(NetworkProtoContext context) {
        return null;
    }

    @Override
    public void initialize() {

    }
}
