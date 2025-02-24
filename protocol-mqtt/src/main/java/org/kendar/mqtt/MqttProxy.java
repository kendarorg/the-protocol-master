package org.kendar.mqtt;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.mqtt.utils.MqttProxySocket;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
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
    public MqttProxy(ByteProtocolSettingsWithLogin settings) {
        super(settings.getConnectionString(), settings.getLogin(), settings.getPassword());
    }

    public MqttProxy() {
        super();
    }

    public MqttProxy(String connectionString, String userId, String password) {
        super(connectionString, userId, password);
    }

    @Override
    protected NetworkProxySocket buildProxyConnection(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        try {
            return new MqttProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getCaller() {

        return "MQTT";
    }
}
