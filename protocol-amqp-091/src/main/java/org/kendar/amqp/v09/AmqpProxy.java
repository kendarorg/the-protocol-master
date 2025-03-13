package org.kendar.amqp.v09;

import org.kendar.amqp.v09.utils.AmqpProxySocket;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.settings.ByteProtocolSettingsWithLogin;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;

@TpmService
public class AmqpProxy extends NetworkProxy {

    @TpmConstructor
    public AmqpProxy(@TpmNamed(tags = "amqp091") ByteProtocolSettingsWithLogin settings) {
        super(settings.getConnectionString(), settings.getLogin(), settings.getPassword());
    }

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
