package org.kendar.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.storage.Storage;
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

public abstract class NetworkProxy<T extends Storage<JsonNode, JsonNode>, K extends NetworkReturnMessage> extends Proxy<T>{
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(NetworkProxy.class);
    protected String connectionString;
    protected String userId;
    protected String password;
    protected int port;
    protected String host;
    protected ExecutorService executor;
    protected AsynchronousChannelGroup group;

    public NetworkProxy(String connectionString, String userId, String password) {
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
            return new ProxyConnection(buildProxyConnection(context,
                    new InetSocketAddress(InetAddress.getByName(host), port), group));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract NetworkProxySocket buildProxyConnection(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group);

    @Override
    public void initialize() {

    }



    public <K> void execute(NetworkProtoContext context, ProxyConnection connection, K of) {
        var req = "{\"type\":\"" + of.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(of) + "}";
        var jsonReq = mapper.toJsonNode(req);
        if (replayer) {
            var item = storage.read(jsonReq, of.getClass().getSimpleName());
            if (item.getOutput() == null && item.getInput() == null) {
                sendBackResponses(storage.readResponses(item.getIndex()));
                return;
            }
            sendBackResponses(storage.readResponses(item.getIndex()));
            return;
        }

        long start = System.currentTimeMillis();

        var sock = (NetworkProxySocket) connection.getConnection();
        sock.write(of, protocol.buildBuffer());
        var res = "{\"type\":null,\"data\":null}";
        long end = System.currentTimeMillis();
        storage.write(
                context.getContextId(),
                mapper.toJsonNode(req)
                , mapper.toJsonNode(res)
                , (end - start), of.getClass().getSimpleName(), "AMQP");
    }

    protected abstract void sendBackResponses(List<StorageItem<JsonNode, JsonNode>> storageItems);
}
