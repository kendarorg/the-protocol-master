package org.kendar.redis;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.ProxyException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.WireProxySocket;
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
    public Resp3Proxy(@TpmNamed(tags = "redis") ByteProtocolSettings settings) {
        super(settings.getConnectionString(), null, null,settings.isStartWithTls());
    }

    public Resp3Proxy(String connectionString, String userId, String password) {
        super(connectionString, userId, password);
    }

    public Resp3Proxy(String connectionString, String userId, String password,boolean startWithTls) {
        super(connectionString, userId, password,startWithTls);
    }

    @Override
    protected WireProxySocket buildProxyConnection(NetworkProtoContext context,
                                                   InetSocketAddress inetSocketAddress,
                                                   AsynchronousChannelGroup group) {
        try {
            return new Resp3ProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port),group);
        } catch (UnknownHostException e) {
            throw new ProxyException(e);
        }
    }


    protected String getCaller() {
        return "RESP3";
    }


}
