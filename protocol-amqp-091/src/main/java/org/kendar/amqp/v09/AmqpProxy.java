package org.kendar.amqp.v09;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicCancel;
import org.kendar.amqp.v09.messages.methods.basic.BasicDeliver;
import org.kendar.amqp.v09.utils.AmqpProxySocket;
import org.kendar.amqp.v09.utils.AmqpStorage;
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

    @Override
    protected String getCaller() {
        return "AMQP";
    }

    @Override
    protected Object getData(Object of) {
        return of;
    }

    @Override
    protected Object buildState(ProtoContext context, JsonNode out, Class<? extends ProtoState> aClass) {
        return mapper.deserialize(out.get("data").toString(), aClass);
    }

    @Override
    protected void sendBackResponses(List<StorageItem<JsonNode, JsonNode>> storageItems) {
        if (storageItems.isEmpty()) return;
        for (var item : storageItems) {
            var out = item.getOutput();
            var clazz = out.get("type").textValue();
            ReturnMessage fr = null;
            int consumeId = -1;
            switch (clazz) {
                case "BasicDeliver":
                    var bd = mapper.deserialize(out.get("data").toString(), BasicDeliver.class);
                    consumeId = bd.getConsumeId();
                    fr = bd;
                    break;
                case "HeaderFrame":
                    var hf = mapper.deserialize(out.get("data").toString(), HeaderFrame.class);
                    consumeId = hf.getConsumeId();
                    fr = hf;
                    break;
                case "BodyFrame":
                    var bf = mapper.deserialize(out.get("data").toString(), BodyFrame.class);
                    consumeId = bf.getConsumeId();
                    fr = bf;
                    break;
                case "BasicCancel":
                    var bc = mapper.deserialize(out.get("data").toString(), BasicCancel.class);
                    consumeId = bc.getConsumeId();
                    fr = bc;
                    break;
            }
            if (fr != null) {
                log.debug("[SERVER][CB]: " + fr.getClass().getSimpleName());
                var ctx = AmqpProtocol.consumeContext.get(consumeId);
                ctx.write(fr);
            } else {
                throw new RuntimeException("MISSING CLASS " + clazz);
            }

        }
    }
}
