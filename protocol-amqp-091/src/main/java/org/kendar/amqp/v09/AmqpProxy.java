package org.kendar.amqp.v09;

import org.kendar.amqp.v09.messages.frames.Frame;
import org.kendar.amqp.v09.utils.AmqpStorage;
import org.kendar.buffers.BBuffer;
import org.kendar.proxy.Proxy;
import org.kendar.proxy.ProxyConnection;
import org.kendar.server.SocketChannel;
import org.kendar.utils.JsonMapper;

import java.io.IOException;
import java.net.*;
import java.util.Base64;

public class AmqpProxy extends Proxy<AmqpStorage> {
    private String connectionString;
    private String userId;
    private String password;
    private int port;
    private boolean replayer;


    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    private String host;

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
            this.replayer=false;
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

    public AmqpProxy(){
        this.replayer=true;
    }

    @Override
    public ProxyConnection connect() {
        if(replayer){
            return new ProxyConnection(null);
        }
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

    public <T extends Frame> T execute(ProxyConnection connection, BBuffer of, T toRead) {
        var req = "{\"type\":\"byte[]\",\"data\":{\"bytes\":\""+ Base64.getEncoder().encode(of.getAll())+"\"}}";
        var jsonReq = mapper.toJsonNode(req);

        if(replayer){
            var item = storage.read(jsonReq,"byte[]");
            var out = item.getOutput();
            return (T)mapper.deserialize(out.get("data").toString(),toRead.getClass());
        }

        long start = System.currentTimeMillis();
        var sock = (SocketChannel)connection.getConnection();
        sock.write(of);
        sock.read(toRead);

        var res =  "{\"type\":\""+toRead.getClass().getSimpleName()+"\",\"data\":"+mapper.serializeCompact(toRead)+"}";
        long end = System.currentTimeMillis();
        storage.write(
                jsonReq
                ,mapper.toJsonNode(res)
                ,(end - start),"byte[]","AMQP");
        return toRead;
    }


    protected static JsonMapper mapper = new JsonMapper();

    public <T extends Frame,K extends Frame> T execute(ProxyConnection connection, K of, T toRead) {
        var req = "{\"type\":\""+of.getClass().getSimpleName()+"\",\"data\":"+mapper.serializeCompact(of)+"}";
        var jsonReq = mapper.toJsonNode(req);
        if(replayer){
            var item = storage.read(jsonReq,of.getClass().getSimpleName());
            var out = item.getOutput();
            return (T)mapper.deserialize(out.get("data").toString(),toRead.getClass());
        }

        long start = System.currentTimeMillis();

        var sock = (SocketChannel)connection.getConnection();
        sock.write(of,protocol.buildBuffer());
        sock.read(toRead);

        var res =  "{\"type\":\""+toRead.getClass().getSimpleName()+"\",\"data\":"+mapper.serializeCompact(toRead)+"}";
        long end = System.currentTimeMillis();
        storage.write(
                jsonReq
                ,mapper.toJsonNode(res)
                ,(end - start),of.getClass().getSimpleName(),"AMQP");
        return toRead;
    }

    public <T extends Frame> void execute(ProxyConnection connection,  T of) {
        var req = "{\"type\":\""+of.getClass().getSimpleName()+"\",\"data\":"+mapper.serializeCompact(of)+"}";
        var jsonReq = mapper.toJsonNode(req);
        if(replayer){
            var item = storage.read(jsonReq,of.getClass().getSimpleName());
            return;
        }

        long start = System.currentTimeMillis();

        var sock = (SocketChannel)connection.getConnection();
        sock.write(of,protocol.buildBuffer());
        var res =  "{\"type\":null,\"data\":null}";
        long end = System.currentTimeMillis();
        storage.write(
                mapper.toJsonNode(req)
                ,mapper.toJsonNode(res)
                ,(end - start),of.getClass().getSimpleName(),"AMQP");
    }
}
