package org.kendar.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.mqtt.fsm.ConnectAck;
import org.kendar.mqtt.fsm.Publish;
import org.kendar.mqtt.utils.MqttProxySocket;
import org.kendar.mqtt.utils.MqttStorage;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.storage.StorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;

public class MqttProxy extends NetworkProxy<MqttStorage> {

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

    @Override
    protected Object getData(Object of) {

        return of;
    }

    @Override
    protected Object buildState(ProtoContext context, JsonNode out, Class<? extends ProtoState> aClass) {

        return mapper.deserialize(out.get("data").toString(), aClass);
    }

    private static final Logger log = LoggerFactory.getLogger(MqttProxy.class);

    @Override
    protected void sendBackResponses(List<StorageItem<JsonNode, JsonNode>> storageItems) {
        if (storageItems.isEmpty()) return;
        for (var item : storageItems) {
            var out = item.getOutput();
            var clazz = out.get("type").textValue();
            ReturnMessage fr = null;
            int consumeId = item.getConnectionId();
            switch (clazz) {
                case "ConnectAck":
                    var ca = mapper.deserialize(out.get("data").toString(), ConnectAck.class);
                    fr = ca;
                    break;
                case "Publish":
                    var pb = mapper.deserialize(out.get("data").toString(), Publish.class);
                    fr = pb;
                    break;
                default:
                    throw new RuntimeException("MISSING "+clazz);

            }
            if (fr != null) {
                log.debug("[SERVER][CB]: " + fr.getClass().getSimpleName());
                var ctx = MqttProtocol.consumeContext.get(consumeId);
                ctx.write(fr);
            } else {
                throw new RuntimeException("MISSING CLASS " + clazz);
            }

        }
    }
}
