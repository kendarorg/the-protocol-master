package org.kendar.amqp.v09;

import org.kendar.amqp.v09.utils.AmqpProxySocket;
import org.kendar.amqp.v09.utils.AmqpStorage;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;

public class AmqpProxy extends NetworkProxy<AmqpStorage> {
    private static final Logger log = LoggerFactory.getLogger(AmqpProxy.class);

    public AmqpProxy(String connectionString, String userId, String password) {
        super(connectionString, userId, password);
    }

    public AmqpProxy() {
        super();
    }

    @Override
    protected NetworkProxySocket buildProxyConnection(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        try {
            return new AmqpProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    protected String getCaller() {
        return "AMQP";
    }

}
