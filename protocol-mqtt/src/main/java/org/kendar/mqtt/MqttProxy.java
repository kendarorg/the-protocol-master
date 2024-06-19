package org.kendar.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.mqtt.utils.MqttStorage;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.storage.StorageItem;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;

public class MqttProxy extends NetworkProxy<MqttStorage> {
    @Override
    protected NetworkProxySocket buildProxyConnection(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        return null;
    }

    @Override
    protected String getCaller() {
        return "";
    }

    @Override
    protected Object getData(Object of) {
        return null;
    }

    @Override
    protected Object buildState(ProtoContext context, JsonNode out, Class<? extends ProtoState> aClass) {
        return null;
    }

    @Override
    protected void sendBackResponses(List<StorageItem<JsonNode, JsonNode>> storageItems) {

    }
}
