package org.kendar.amqp.v09;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.amqp.v09.messages.frames.BodyFrame;
import org.kendar.amqp.v09.messages.frames.Frame;
import org.kendar.amqp.v09.messages.frames.HeaderFrame;
import org.kendar.amqp.v09.messages.methods.basic.BasicDeliver;
import org.kendar.amqp.v09.utils.AmqpStorage;
import org.kendar.amqp.v09.utils.ProxySocket;
import org.kendar.buffers.BBuffer;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Base64;
import java.util.List;

public class AmqpProxy extends Proxy<AmqpStorage> {

    protected static JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(AmqpProxy.class);
    private String connectionString;
    private String userId;
    private String password;
    private int port;
    private final boolean replayer;
    private String host;

    public AmqpProxy(String connectionString, String userId, String password) {
        try {
            this.replayer = false;
            var uri = new URI(connectionString);
            this.connectionString = connectionString;
            this.port = uri.getPort();
            this.host = uri.getHost();
            this.userId = userId;
            this.password = password;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public AmqpProxy() {
        this.replayer = true;
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
                    new InetSocketAddress(InetAddress.getByName(host), port)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize() {
    }

    public <T extends Frame> T execute(NetworkProtoContext context, ProxyConnection connection, BBuffer of, T toRead) {
        var req = "{\"type\":\"byte[]\",\"data\":{\"bytes\":\"" + Base64.getEncoder().encode(of.getAll()) + "\"}}";
        var jsonReq = mapper.toJsonNode(req);

        if (replayer) {
            var item = storage.read(jsonReq, "byte[]");
            writeResponses(storage.readResponses(item.getIndex()));
            var out = item.getOutput();
            return (T) mapper.deserialize(out.get("data").toString(), toRead.getClass());

        }

        long start = System.currentTimeMillis();
        var sock = (ProxySocket) connection.getConnection();
        sock.write(of);
        sock.read(toRead);

        var res = "{\"type\":\"" + toRead.getClass().getSimpleName() + "\",\"data\":" + mapper.serializeCompact(toRead) + "}";
        long end = System.currentTimeMillis();
        storage.write(
                jsonReq
                , mapper.toJsonNode(res)
                , (end - start), "byte[]", "AMQP");
        return toRead;
    }

    private void writeResponses(List<StorageItem<JsonNode, JsonNode>> storageItems) {
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
            }
            log.debug("[SERVER][CB]: " + fr.getClass().getSimpleName());
            var ctx = AmqpProtocol.consumeContext.get(consumeId);
            ctx.write(fr);
        }
    }


    public <T extends Frame, K extends Frame> T execute(NetworkProtoContext context, ProxyConnection connection, K of, T toRead) {
        var req = "{\"type\":\"" + of.getClass().getSimpleName() + "\",\"data\":" + mapper.serializeCompact(of) + "}";
        var jsonReq = mapper.toJsonNode(req);
        if (replayer) {
            var item = storage.read(jsonReq, of.getClass().getSimpleName());
            writeResponses(storage.readResponses(item.getIndex()));
            item.getIndex();

            var out = item.getOutput();
            return (T) mapper.deserialize(out.get("data").toString(), toRead.getClass());

        }

        long start = System.currentTimeMillis();

        var sock = (ProxySocket) connection.getConnection();
        var bufferToWrite = protocol.buildBuffer();
        sock.write(of, bufferToWrite);
        sock.read(toRead);

        var res = "{\"type\":\"" + toRead.getClass().getSimpleName() + "\",\"data\":" + mapper.serializeCompact(toRead) + "}";
        long end = System.currentTimeMillis();

        storage.write(
                jsonReq
                , mapper.toJsonNode(res)
                , (end - start), of.getClass().getSimpleName(), "AMQP");
        return toRead;
    }

    public <T extends Frame> void execute(NetworkProtoContext context, ProxyConnection connection, T of) {
        var req = "{\"type\":\"" + of.getClass().getSimpleName() + "\",\"data\":" + mapper.serializeCompact(of) + "}";
        var jsonReq = mapper.toJsonNode(req);
        if (replayer) {
            var item = storage.read(jsonReq, of.getClass().getSimpleName());
            writeResponses(storage.readResponses(item.getIndex()));
            return;
        }

        long start = System.currentTimeMillis();

        var sock = (ProxySocket) connection.getConnection();
        sock.write(of, protocol.buildBuffer());
        var res = "{\"type\":null,\"data\":null}";
        long end = System.currentTimeMillis();
        storage.write(
                mapper.toJsonNode(req)
                , mapper.toJsonNode(res)
                , (end - start), of.getClass().getSimpleName(), "AMQP");
    }
}
