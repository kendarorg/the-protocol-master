package org.kendar.mqtt;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.ProxyException;
import org.kendar.mqtt.utils.MqttProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.WireProxySocket;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;

@TpmService
public class MqttProxy extends NetworkProxy {

    private static final Logger log = LoggerFactory.getLogger(MqttProxy.class);

    @TpmConstructor
    public MqttProxy(@TpmNamed(tags = "mqtt") ByteProtocolSettingsWithLogin settings) {
        super(settings.getConnectionString(), settings.getLogin(), settings.getPassword(),settings.isStartWithTls());
    }

    public MqttProxy(String connectionString, String userId, String password) {
        super(connectionString, userId, password);
    }
    public MqttProxy() {
        super();
    }

    public MqttProxy(String connectionString, String userId, String password,boolean startWithTls) {
        super(connectionString, userId, password,startWithTls);
    }

    @Override
    protected WireProxySocket buildProxyConnection(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        try {
            return new MqttProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group);
        } catch (UnknownHostException e) {
            throw new ProxyException(e);
        }
    }

    @Override
    protected String getCaller() {

        return "MQTT";
    }
}
