package org.kendar.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.buffers.BBuffer;
import org.kendar.filters.ProtocolPhase;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.storage.Storage;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.net.*;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class NetworkProxy<T extends Storage<JsonNode, JsonNode>> extends Proxy<T> {
    protected static final JsonMapper mapper = new JsonMapper();
    protected String connectionString;
    protected String userId;
    protected String password;
    protected int port;
    protected String host;
    protected ExecutorService executor;
    protected AsynchronousChannelGroup group;

    public NetworkProxy() {
        this.replayer = true;
        init();
    }

    public NetworkProxy(String connectionString, String userId, String password) {
        try {
            this.replayer = false;
            this.connectionString = connectionString;
            if (connectionString != null && !connectionString.isEmpty()) {
                var uri = new URI(connectionString);
                this.port = uri.getPort();
                this.host = uri.getHost();
            }
            this.userId = userId;
            this.password = password;
            init();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    /**
     * Setup the executor and the TCP channel
     */
    private void init() {
        executor = Executors.newCachedThreadPool();
        try {
            group = AsynchronousChannelGroup.withThreadPool(executor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create the connection
     *
     * @param context
     * @return
     */
    @Override
    public ProxyConnection connect(NetworkProtoContext context) {
        if (replayer) {
            return new ProxyConnection(null);
        }
        try {
            @SuppressWarnings("resource")
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

    /**
     * Build the specific connection with custom parameter and class
     *
     * @param context
     * @param inetSocketAddress
     * @param group
     * @return
     */
    protected abstract NetworkProxySocket buildProxyConnection(NetworkProtoContext context, InetSocketAddress inetSocketAddress, AsynchronousChannelGroup group);

    @Override
    public void initialize() {

    }


    /**
     * Execute with no return
     *
     * @param context
     * @param connection
     * @param of
     */
    public <K extends NetworkReturnMessage> void sendAndForget(NetworkProtoContext context, ProxyConnection connection, K of) {

        var req = "{\"type\":\"" + of.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(of)) + "}";
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

        var index = storage.generateIndex();
        long start = System.currentTimeMillis();
        var filterContext = new FilterContext(getCaller(),of.getClass().getSimpleName(),start, context);
        for (var filter : getFilters(ProtocolPhase.PRE_CALL, of, new Object())) {
            if (filter.handle(filterContext, ProtocolPhase.PRE_CALL, of, null)) {
                return;
            }
        }


        var sock = (NetworkProxySocket) connection.getConnection();
        sock.write(of, getProtocol().buildBuffer());
        var jsonRes = mapper.toJsonNode("{\"type\":null,\"data\":null}");
        long end = System.currentTimeMillis();
        for (var filter : getFilters(ProtocolPhase.POST_CALL, of, new Object())) {
            if (filter.handle(filterContext, ProtocolPhase.POST_CALL, of, null)) {
                break;
            }
        }
        storage.write(
                index,
                context.getContextId(),
                jsonReq
                , jsonRes
                , (end - start), of.getClass().getSimpleName(), getCaller());


    }

    /**
     * Set the name of the protocol to store when recording
     *
     * @return
     */
    protected abstract String getCaller();

    /**
     * Given a event to read return the data to serialize
     *
     * @param of
     * @return
     */
    protected abstract Object getData(Object of);

    /**
     * Run expecting a message and a return value
     *
     * @param context
     * @param connection
     * @param of
     * @param toRead
     * @param <T>
     * @param <K>
     * @return
     */
    public <T extends ProtoState, K extends ReturnMessage> T sendAndExpect(
            NetworkProtoContext context,
            ProxyConnection connection,
            K of,
            T toRead) {
        return sendAndExpect(context, connection, of, toRead, false);
    }

    public <T extends ProtoState, K extends ReturnMessage> T sendAndExpect(
            NetworkProtoContext context,
            ProxyConnection connection,
            K of,
            T toRead,
            boolean optional) {
        var req = "{\"type\":\"" + of.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(of)) + "}";
        var jsonReq = mapper.toJsonNode(req);

        if (replayer) {
            var item = storage.read(jsonReq, of.getClass().getSimpleName());
            if (item.getOutput() == null && item.getInput() == null) {
                sendBackResponses(storage.readResponses(item.getIndex()));
                return toRead;
            }
            sendBackResponses(storage.readResponses(item.getIndex()));

            var out = item.getOutput();
            if (context.isUseCallDurationTimes()) {
                Sleeper.sleep(item.getDurationMs());
            }
            return (T) buildState(context, out, toRead.getClass());

        }
        var index = storage.generateIndex();

        long start = System.currentTimeMillis();
        var filterContext = new FilterContext(getCaller(),of.getClass().getSimpleName(),start, context);

        for (var filter : getFilters(ProtocolPhase.PRE_CALL, of, toRead)) {
            if (filter.handle(filterContext,ProtocolPhase.PRE_CALL, of, toRead)) {
                return toRead;
            }
        }

        var sock = (NetworkProxySocket) connection.getConnection();
        var bufferToWrite = getProtocol().buildBuffer();
        sock.write(of, bufferToWrite);
        var returnMessages = sock.read(toRead, optional);
        for (var item : returnMessages) {
            if (toRead.getClass() == item.getClass()) {
                toRead = (T) item;
                break;
            }
        }

        for (var filter : getFilters(ProtocolPhase.POST_CALL, of, toRead)) {
            if (filter.handle(filterContext, ProtocolPhase.POST_CALL, of, toRead)) {
                break;
            }
        }

        var res = "{\"type\":\"" + toRead.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(toRead)) + "}";
        long end = System.currentTimeMillis();

        storage.write(
                index,
                context.getContextId(),
                jsonReq
                , mapper.toJsonNode(res)
                , (end - start), of.getClass().getSimpleName(), getCaller());
        return toRead;
    }

    /**
     * Build the state that will be rendered to the client based on the json serialized data
     *
     * @param context
     * @param out
     * @param aClass
     * @return
     */
    protected abstract Object buildState(ProtoContext context, JsonNode out, Class<? extends ProtoState> aClass);

    /**
     * Execute with return data (proto state to be precise)
     *
     * @param context
     * @param connection
     * @param of
     * @param toRead
     * @param <J>
     * @return
     */
    public <J extends ProtoState> J sendBytesAndExpect(NetworkProtoContext context, ProxyConnection connection, BBuffer of, J toRead) {
        return sendBytesAndExpect(context, connection, of, toRead, false);
    }

    public <J extends ProtoState> J sendBytesAndExpect(NetworkProtoContext context, ProxyConnection connection, BBuffer of, J toRead, boolean optional) {
        var req = "{\"type\":\"byte[]\",\"data\":{\"bytes\":\"" + Base64.getEncoder().encode(of.getAll()) + "\"}}";
        var jsonReq = mapper.toJsonNode(req);

        if (replayer) {
            var item = storage.read(jsonReq, "byte[]");
            if (item.getOutput() == null && item.getInput() == null) {
                sendBackResponses(storage.readResponses(item.getIndex()));
                return toRead;
            }
            sendBackResponses(storage.readResponses(item.getIndex()));
            var out = item.getOutput();
            if (context.isUseCallDurationTimes()) {
                Sleeper.sleep(item.getDurationMs());
            }
            return (J) buildState(context, out, toRead.getClass());

        }
        var index = storage.generateIndex();
        long start = System.currentTimeMillis();
        var filterContext = new FilterContext(getCaller(),"byte[]",start,context);

        for (var filter : getFilters(ProtocolPhase.PRE_CALL, of, toRead)) {
            if (filter.handle(filterContext, ProtocolPhase.PRE_CALL, of, toRead)) {
                return toRead;
            }
        }

        var sock = (NetworkProxySocket) connection.getConnection();
        sock.write(of);
        sock.read(toRead, optional);

        for (var filter : getFilters(ProtocolPhase.POST_CALL, of, toRead)) {
            if (filter.handle(filterContext, ProtocolPhase.POST_CALL, of, toRead)) {
                break;
            }
        }

        var res = "{\"type\":\"" + toRead.getClass().getSimpleName() + "\",\"data\":" + mapper.serialize(getData(toRead)) + "}";
        long end = System.currentTimeMillis();
        storage.write(
                index,
                context.getContextId(),
                jsonReq
                , mapper.toJsonNode(res)
                , (end - start), "byte[]", getCaller());


        return toRead;
    }

    /**
     * In case of push messages from the recording, this translate the storage item involved
     * in the correct message. Finds the correct originating connection (From the connection
     * id and the list of connections just created) and write the result to the client resulting
     * in the emulation of an async response
     *
     * @param storageItems
     */
    protected abstract void sendBackResponses(List<StorageItem<JsonNode, JsonNode>> storageItems);

    public void respond(Object publish,FilterContext filterContext) {
        for (var filter : getFilters(ProtocolPhase.ASYNC_RESPONSE, new Object(), publish)) {
            if (filter.handle(filterContext, ProtocolPhase.ASYNC_RESPONSE, null, publish)) {
                break;
            }
        }
    }
}
