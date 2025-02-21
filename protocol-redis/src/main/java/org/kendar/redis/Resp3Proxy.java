package org.kendar.redis;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.redis.utils.Resp3ProxySocket;
import org.kendar.settings.ByteProtocolSettings;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;

@TpmService()
public class Resp3Proxy extends NetworkProxy {
    public Resp3Proxy() {

    }

    @TpmConstructor
    public Resp3Proxy(ByteProtocolSettings settings) {
        super(settings.getConnectionString(), null, null);
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
