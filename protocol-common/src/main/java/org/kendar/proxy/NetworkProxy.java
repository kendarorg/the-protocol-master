package org.kendar.proxy;

import org.kendar.buffers.BBuffer;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.utils.JsonMapper;

import java.io.IOException;
import java.net.*;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class NetworkProxy extends Proxy {
    protected static final JsonMapper mapper = new JsonMapper();
    protected String connectionString;
    protected String userId;
    protected String password;
    protected int port;
    protected String host;
    protected ExecutorService executor;
    protected AsynchronousChannelGroup group;

    public NetworkProxy() {
        super();
        init();
    }

    public NetworkProxy(String connectionString, String userId, String password) {
        super();
        try {
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
            return new ProxyConnection(null);
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


        long start = System.currentTimeMillis();
        var pluginContext = new PluginContext(getCaller(), of.getClass().getSimpleName(), start, context);
        for (var plugin : getPlugins(ProtocolPhase.PRE_CALL, of, new Object())) {
            if (plugin.handle(pluginContext, ProtocolPhase.PRE_CALL, of, null)) {
                return;
            }
        }

        var sock = (NetworkProxySocket) connection.getConnection();
        sock.write(of, getProtocol().buildBuffer());
        for (var plugin : getPlugins(ProtocolPhase.POST_CALL, of, new Object())) {
            if (plugin.handle(pluginContext, ProtocolPhase.POST_CALL, of, null)) {
                break;
            }
        }

    }

    /**
     * Set the name of the protocol to store when recording
     *
     * @return
     */
    protected abstract String getCaller();


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

        long start = System.currentTimeMillis();
        var pluginContext = new PluginContext(getCaller(), of.getClass().getSimpleName(), start, context);

        for (var plugin : getPlugins(ProtocolPhase.PRE_CALL, of, toRead)) {
            if (plugin.handle(pluginContext, ProtocolPhase.PRE_CALL, of, toRead)) {
                return toRead;
            }
        }

        var sock = (NetworkProxySocket) connection.getConnection();
        if (sock == null) {
            return null;
        }
        var bufferToWrite = getProtocol().buildBuffer();
        sock.write(of, bufferToWrite);
        var returnMessages = sock.read(toRead, optional);
        for (var item : returnMessages) {
            if (toRead.getClass() == item.getClass()) {
                toRead = (T) item;
                break;
            }
        }

        for (var plugin : getPlugins(ProtocolPhase.POST_CALL, of, toRead)) {
            if (plugin.handle(pluginContext, ProtocolPhase.POST_CALL, of, toRead)) {
                break;
            }
        }
        return toRead;
    }

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

        long start = System.currentTimeMillis();
        var pluginContext = new PluginContext(getCaller(), "byte[]", start, context);

        for (var plugin : getPlugins(ProtocolPhase.PRE_CALL, of, toRead)) {
            if (plugin.handle(pluginContext, ProtocolPhase.PRE_CALL, of, toRead)) {
                return toRead;
            }
        }

        var sock = (NetworkProxySocket) connection.getConnection();
        sock.write(of);
        sock.read(toRead, optional);

        for (var plugin : getPlugins(ProtocolPhase.POST_CALL, of, toRead)) {
            if (plugin.handle(pluginContext, ProtocolPhase.POST_CALL, of, toRead)) {
                break;
            }
        }
        return toRead;
    }

    public void respond(Object publish, PluginContext pluginContext) {
        for (var plugin : getPlugins(ProtocolPhase.ASYNC_RESPONSE, new Object(), publish)) {
            if (plugin.handle(pluginContext, ProtocolPhase.ASYNC_RESPONSE, null, publish)) {
                break;
            }
        }
    }

}
