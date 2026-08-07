package org.kendar.amqp.v10;

import org.kendar.amqp.v10.utils.Amqp10ProxySocket;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.ProxyException;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.WireProxySocket;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;

@Extension
@TpmService
public class Amqp10Proxy extends NetworkProxy implements ExtensionPoint {

    @TpmConstructor
    public Amqp10Proxy(@TpmNamed(tags = "amqp10") ByteProtocolSettingsWithLogin settings) {
        super(settings.getConnectionString(), settings.getLogin(), settings.getPassword(),
                settings.isStartWithTls());
    }

    public Amqp10Proxy(String connectionString, String userId, String password, boolean startWithTls) {
        super(connectionString, userId, password, startWithTls);
    }

    public Amqp10Proxy(String connectionString, String userId, String password) {
        super(connectionString, userId, password);
    }

    public Amqp10Proxy() {
        super();
    }

    @Override
    protected WireProxySocket buildProxyConnection(NetworkProtoContext context,
                                                   InetSocketAddress inetSocketAddress,
                                                   AsynchronousChannelGroup group) {
        try {
            return new Amqp10ProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group);
        } catch (UnknownHostException e) {
            throw new ProxyException(e);
        }
    }

    protected String getCaller() {
        return "AMQP10";
    }
}
