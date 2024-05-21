package org.kendar.redis;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.redis.fsm.Resp3PullState;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.utils.Resp3ProxySocket;
import org.kendar.redis.utils.Resp3Storage;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OldResp3Proxy extends Proxy<Resp3Storage> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(OldResp3Proxy.class);
    private String connectionString;
    private String userId;
    private String password;
    private int port;
    private String host;
    private ExecutorService executor;
    private AsynchronousChannelGroup group;

    public OldResp3Proxy(String connectionString, String userId, String password) {
        try {
            this.replayer = false;
            var uri = new URI(connectionString);
            this.connectionString = connectionString;
            this.port = uri.getPort();
            this.host = uri.getHost();
            this.userId = userId;
            this.password = password;
            init();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public OldResp3Proxy(Resp3Storage jdbcStorage) {
        this("redis://none:1", null, null);
        setStorage(jdbcStorage);
        this.replayer = true;

    }

    public OldResp3Proxy() {
        this.replayer = true;
        init();
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    private void init() {
        executor = Executors.newCachedThreadPool();
        try {
            group = AsynchronousChannelGroup.withThreadPool(executor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProxyConnection connect(NetworkProtoContext context) {
        if (replayer) {
            return new ProxyConnection(null);
        }
        try {
            var connection = new Socket();
            connection.setSoTimeout(60 * 1000);
            connection.setKeepAlive(true);
            connection.setTcpNoDelay(true);
            connection.connect(new InetSocketAddress(InetAddress.getByName(host), port));
//            return new ProxyConnection(new Resp3ProxySocket(context,
//                    new InetSocketAddress(InetAddress.getByName(host), port), group, storage));
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize() {
    }

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

    public ReturnMessage execute(Resp3Context context, ProxyConnection connection, Resp3Message event, ProtoState toRead) {
        var req = "";
        req = "{\"type\":\"" + ((List<Object>) event.getData()).get(0) + "\",\"data\":" + mapper.serialize(event.getData()) + "}";
        var jsonReq = mapper.toJsonNode(req);
        if (replayer) {
            var item = storage.read(jsonReq, (String) ((List<Object>) event.getData()).get(0));
            if (item.getOutput() == null && item.getInput() == null) {
                sendBackResponses(storage.readResponses(item.getIndex()));
                return (ReturnMessage) toRead;
            }
            sendBackResponses(storage.readResponses(item.getIndex()));

            var out = item.getOutput();
            return new Resp3Message(context, null, out.get("data"));

        }

        var index = storage.generateIndex();
        long start = System.currentTimeMillis();

        var sock = (Resp3ProxySocket) connection.getConnection();
        var bufferToWrite = protocol.buildBuffer();
        try {
            sock.write(event, bufferToWrite);
            sock.read(toRead);
        } catch (Exception ex) {
            log.error("CANNOT READ SHIT", ex);
        }
        var content = ((Resp3PullState) toRead).getEvent().getData();
        var res = "{\"type\":\"" + content.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(content) + "}";
        long end = System.currentTimeMillis();

        storage.write(
                index,
                context.getContextId(),
                jsonReq
                , mapper.toJsonNode(res)
                , (end - start), (String) ((List<Object>) event.getData()).get(0), "RESP3");
        return (ReturnMessage) toRead;
    }
}
