package org.kendar.server;


import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.kendar.server.exchange.HttpContextImpl;
import org.kendar.server.utils.ServerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

public class KendarHttpServer extends HttpServer {
    ServerImpl server;

    KendarHttpServer() throws IOException {
        this(new InetSocketAddress(80), 0);
    }

    KendarHttpServer(InetSocketAddress addr, int backlog) throws IOException {
        this.server = new ServerImpl(this, "http", addr, backlog);
    }

    public void bind(InetSocketAddress addr, int backlog) throws IOException {
        this.server.bind(addr, backlog);
    }

    public void start() {
        this.server.start();
    }

    public Executor getExecutor() {
        return this.server.getExecutor();
    }

    public void setExecutor(Executor executor) {
        this.server.setExecutor(executor);
    }

    public void stop(int delay) {
        this.server.stop(delay);
    }

    public HttpContextImpl createContext(String path, HttpHandler handler) {
        return this.server.createContext(path, handler);
    }

    public HttpContextImpl createContext(String path) {
        return this.server.createContext(path);
    }

    public void removeContext(String path) throws IllegalArgumentException {
        this.server.removeContext(path);
    }

    public void removeContext(HttpContext context) throws IllegalArgumentException {
        this.server.removeContext(context);
    }

    public InetSocketAddress getAddress() {
        return this.server.getAddress();
    }
}
