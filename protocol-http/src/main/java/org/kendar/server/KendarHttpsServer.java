package org.kendar.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.kendar.server.exchange.HttpContextImpl;
import org.kendar.server.utils.ServerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

public class KendarHttpsServer extends HttpsServer {

    ServerImpl server;

    KendarHttpsServer() throws IOException {
        this(new InetSocketAddress(443), 0);
    }

    public KendarHttpsServer(
            InetSocketAddress addr, int backlog
    ) throws IOException {
        server = new ServerImpl(this, "https", addr, backlog);
    }

    public HttpsConfigurator getHttpsConfigurator() {
        return server.getHttpsConfigurator();
    }

    public void setHttpsConfigurator(HttpsConfigurator config) {
        server.setHttpsConfigurator(config);
    }

    public void bind(InetSocketAddress addr, int backlog) throws IOException {
        server.bind(addr, backlog);
    }

    public void start() {
        server.start();
    }

    public Executor getExecutor() {
        return server.getExecutor();
    }

    public void setExecutor(Executor executor) {
        server.setExecutor(executor);
    }

    public void stop(int delay) {
        server.stop(delay);
    }

    public HttpContextImpl createContext(String path, HttpHandler handler) {
        return server.createContext(path, handler);
    }

    public HttpContextImpl createContext(String path) {
        return server.createContext(path);
    }

    public void removeContext(String path) throws IllegalArgumentException {
        server.removeContext(path);
    }

    public void removeContext(HttpContext context) throws IllegalArgumentException {
        server.removeContext(context);
    }

    public InetSocketAddress getAddress() {
        return server.getAddress();
    }
}
