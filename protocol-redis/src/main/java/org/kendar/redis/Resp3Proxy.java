package org.kendar.redis;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.redis.utils.Resp3ProxySocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;

public class Resp3Proxy extends NetworkProxy {
    public Resp3Proxy() {

    }

    public Resp3Proxy(String connectionString, String userId, String password) {
        super(connectionString, userId, password);
    }

    @Override
    protected NetworkProxySocket buildProxyConnection(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        try {
            return new Resp3ProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    protected String getCaller() {
        return "RESP3";
    }


}
