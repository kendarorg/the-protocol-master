package org.kendar.redis;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;

public class Resp3Proxy extends Proxy<Resp3Storage> {
    private final String host;
    private final Integer port;

    public Resp3Proxy(String host, Integer port) {

        this.host = host;
        this.port = port;
    }

    @Override
    public ProxyConnection connect(NetworkProtoContext context) {
        return null;
    }

    @Override
    public void initialize() {

    }
}
