package org.kendar.amqp.v09;

import org.kendar.amqp.v09.messages.frames.Frame;
import org.kendar.amqp.v09.utils.AmqpStorage;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.server.SocketChannel;

import java.io.IOException;
import java.net.*;

public class AmqpProxy extends Proxy<AmqpStorage> {
    private final String connectionString;
    private final String userId;
    private final String password;
    private final int port;

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    private final String host;

    public String getConnectionString() {
        return connectionString;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public AmqpProxy(String connectionString, String userId, String password) {
        try {
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

    @Override
    public ProxyConnection connect() {
        try {
            var connection = new Socket();
            connection.setSoTimeout(60*1000);
            connection.setKeepAlive(true);
            connection.setTcpNoDelay(true);
            connection.connect(new InetSocketAddress(InetAddress.getByName(host),port));
            return new ProxyConnection(new SocketChannel(connection));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize() {

    }

    public Frame execute(Frame input){
        return null;
    }
}
