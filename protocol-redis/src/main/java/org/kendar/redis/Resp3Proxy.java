package org.kendar.redis;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.NetworkProxy;
import org.kendar.proxy.NetworkProxySocket;
import org.kendar.redis.fsm.Resp3Response;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.utils.Resp3ProxySocket;
import org.kendar.redis.utils.Resp3Storage;
import org.kendar.storage.StorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;

public class Resp3Proxy extends NetworkProxy<Resp3Storage> {

    private static final Logger log = LoggerFactory.getLogger(Resp3Proxy.class);

    public Resp3Proxy(String connectionString, String userId, String password) {
        super(connectionString, userId, password);
        Resp3Protocol.consumeContext.clear();
    }

    public Resp3Proxy() {
        super();
        Resp3Protocol.consumeContext.clear();
    }

    @Override
    protected NetworkProxySocket buildProxyConnection(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group) {
        try {
            return new Resp3ProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    protected String getCaller() {
        return "RESP3";
    }

    @Override
    protected Object getData(Object of) {
        if (of instanceof Resp3Message) {
            return ((Resp3Message) of).getData();
        }
        if (of instanceof Resp3Response) {
            return getData(((Resp3Response) of).getEvent());
        }
        return of;
    }

    protected Object buildState(ProtoContext context, JsonNode out, Class<? extends ProtoState> aClass) {
        var res = new Resp3Response();
        res.execute(new Resp3Message(context, null, out.get("data")));
        return res;
    }

    @Override
    protected void sendBackResponses(List<StorageItem<JsonNode, JsonNode>> storageItems) {
        if (storageItems.isEmpty()) return;
        for (var item : storageItems) {
            var out = item.getOutput();
            var type = out.get("type").textValue();

            int connectionId = item.getConnectionId();
            if (type.equalsIgnoreCase("RESPONSE")) {
                log.debug("[SERVER][CB]: RESPONSE");
                var ctx = Resp3Protocol.consumeContext.get(connectionId);

                ReturnMessage fr = new Resp3Message(ctx, null, out.get("data"));
                ctx.write(fr);
            } else {
                throw new RuntimeException("MISSING RESPONSE_CLASS");
            }

        }
    }
}
