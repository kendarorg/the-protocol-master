package org.kendar.kafka;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.ProxyException;
import org.kendar.kafka.utils.KafkaProxySocket;
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
public class KafkaProxy extends NetworkProxy implements ExtensionPoint {

    @TpmConstructor
    public KafkaProxy(@TpmNamed(tags = "kafka") ByteProtocolSettingsWithLogin settings) {
        super(settings.getConnectionString(), settings.getLogin(), settings.getPassword(),
                settings.isStartWithTls());
    }

    public KafkaProxy(String connectionString, String userId, String password, boolean startWithTls) {
        super(connectionString, userId, password, startWithTls);
    }

    public KafkaProxy(String connectionString, String userId, String password) {
        super(connectionString, userId, password);
    }

    public KafkaProxy() {
        super();
    }

    @Override
    protected WireProxySocket buildProxyConnection(NetworkProtoContext context,
                                                   InetSocketAddress inetSocketAddress,
                                                   AsynchronousChannelGroup group) {
        try {
            return new KafkaProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group);
        } catch (UnknownHostException e) {
            throw new ProxyException(e);
        }
    }

    protected String getCaller() {
        return "KAFKA";
    }
}
