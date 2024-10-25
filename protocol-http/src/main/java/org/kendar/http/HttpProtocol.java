package org.kendar.http;


import com.sun.net.httpserver.HttpServer;
import org.kendar.storage.BaseStorage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpProtocol  {

    public HttpProtocol(int port, BaseStorage storage) {

    }


    public static void main(String[] args) throws IOException {
        var port = 9092;
        var backlog = 50;
        InetSocketAddress address = new InetSocketAddress(port);

        var httpServer = HttpServer.create(address, backlog);
        httpServer.createContext("/", new GlobalHandler());
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
    }
}
