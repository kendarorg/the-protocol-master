package org.kendar.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.mqtt.utils.MqttProxySocket;
import org.kendar.mqtt.utils.MqttStorage;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.storage.StorageItem;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;

public class MqttProxy extends NetworkProxy<MqttStorage> {

    public MqttProxy(){
        super(null,null,null);
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

    @Override
    protected Object getData(Object of) {

        return of;
    }

    @Override
    protected Object buildState(ProtoContext context, JsonNode out, Class<? extends ProtoState> aClass) {

        return null;
    }

    @Override
    protected void sendBackResponses(List<StorageItem<JsonNode, JsonNode>> storageItems) {
        System.out.println("TEST");
    }
}
