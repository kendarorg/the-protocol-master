package org.kendar.redis;

import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.redis.fsm.Resp3PullState;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.utils.ProxySocket;
import org.kendar.redis.utils.Resp3Storage;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Resp3Proxy extends Proxy<Resp3Storage> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(Resp3Proxy.class);
    private String connectionString;
    private String userId;
    private String password;
    private int port;
    private String host;
    private ExecutorService executor;
    private AsynchronousChannelGroup group;

    public Resp3Proxy(String connectionString, String userId, String password) {
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

    public Resp3Proxy() {
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
            return new ProxyConnection(new ProxySocket(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize() {
    }

//    private void writeResponses(List<StorageItem<JsonNode, JsonNode>> storageItems) {
//        if (storageItems.isEmpty()) return;
//        for (var item : storageItems) {
//            var out = item.getOutput();
//            var clazz = out.get("type").textValue();
//            ReturnMessage fr = null;
//            int consumeId = -1;
//            switch (clazz) {
//                case "BasicDeliver":
//                    var bd = mapper.deserialize(out.get("data").toString(), BasicDeliver.class);
//                    consumeId = bd.getConsumeId();
//                    fr = bd;
//                    break;
//                case "HeaderFrame":
//                    var hf = mapper.deserialize(out.get("data").toString(), HeaderFrame.class);
//                    consumeId = hf.getConsumeId();
//                    fr = hf;
//                    break;
//                case "BodyFrame":
//                    var bf = mapper.deserialize(out.get("data").toString(), BodyFrame.class);
//                    consumeId = bf.getConsumeId();
//                    fr = bf;
//                    break;
//                case "BasicCancel":
//                    var bc = mapper.deserialize(out.get("data").toString(), BasicCancel.class);
//                    consumeId = bc.getConsumeId();
//                    fr = bc;
//                    break;
//            }
//            if (fr != null) {
//                log.debug("[SERVER][CB]: " + fr.getClass().getSimpleName());
//                var ctx = AmqpProtocol.consumeContext.get(consumeId);
//                ctx.write(fr);
//            } else {
//                throw new RuntimeException("MISSING CLASS " + clazz);
//            }
//
//        }
//    }

    public ReturnMessage execute(Reps3Context context, ProxyConnection connection, Resp3Message event, ProtoState toRead) {
        var req = "";
        req = "{\"type\":\"" + ((List<Object>)event.getData()).get(0) + "\",\"data\":" + mapper.serialize(event.getData()) + "}";
        var jsonReq = mapper.toJsonNode(req);
        if (replayer) {
//            var item = storage.read(jsonReq,  (String)((List<Object>)event.getData()).get(0));
//            if (item.getOutput() == null && item.getInput() == null) {
//                writeResponses(storage.readResponses(item.getIndex()));
//                return toRead;
//            }
//            writeResponses(storage.readResponses(item.getIndex()));
//
//            var out = item.getOutput();
//            return (T) mapper.deserialize(out.get("data").toString(), toRead.getClass());

        }

        long start = System.currentTimeMillis();

        var sock = (ProxySocket) connection.getConnection();
        var bufferToWrite = protocol.buildBuffer();
        try {
            sock.write(event, bufferToWrite);
            sock.read(toRead);
        }catch (Exception ex){
            log.error("CANNOT READ SHIT",ex);
        }
        var content = ((Resp3PullState)toRead).getEvent().getData();
        var res = "{\"type\":\"" + content.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(content) + "}";
        long end = System.currentTimeMillis();

        storage.write(
                context.getContextId(),
                jsonReq
                , mapper.toJsonNode(res)
                , (end - start), (String)((List<Object>)event.getData()).get(0), "RESP3");
        return (ReturnMessage) toRead;
    }
}
