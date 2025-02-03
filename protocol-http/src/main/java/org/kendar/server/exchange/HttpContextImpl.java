package org.kendar.server.exchange;

import com.sun.net.httpserver.*;
import org.kendar.server.utils.ServerImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HttpContextImpl extends HttpContext {

    private final String path;
    private final String protocol;
    private final ServerImpl server;
    //private final AuthFilter authfilter;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    /* system filters, not visible to applications */
    private final List<Filter> sfilters = new CopyOnWriteArrayList<>();
    /* user filters, set by applications */
    private final List<Filter> ufilters = new CopyOnWriteArrayList<>();
    private Authenticator authenticator;
    private HttpHandler handler;

    /**
     * constructor is package private.
     */
    public HttpContextImpl(String protocol, String path, HttpHandler cb, ServerImpl server) {
        if (path == null || protocol == null || path.isEmpty() || path.charAt(0) != '/') {
            throw new IllegalArgumentException("Illegal value for path or protocol");
        }
        this.protocol = protocol.toLowerCase();
        this.path = path;
        if (!this.protocol.equals("http") && !this.protocol.equals("https")) {
            throw new IllegalArgumentException("Illegal value for protocol");
        }
        this.handler = cb;
        this.server = server;
    }

    /**
     * returns the handler for this context
     *
     * @return the HttpHandler for this context
     */
    public HttpHandler getHandler() {
        return handler;
    }

    public void setHandler(HttpHandler h) {
        if (h == null) {
            throw new NullPointerException("Null handler parameter");
        }
        if (handler != null) {
            throw new IllegalArgumentException("handler already set");
        }
        handler = h;
    }

    /**
     * returns the path this context was created with
     *
     * @return this context's path
     */
    public String getPath() {
        return path;
    }

    /**
     * returns the server this context was created with
     *
     * @return this context's server
     */
    public HttpServer getServer() {
        return server.getWrapper();
    }

    public ServerImpl getServerImpl() {
        return server;
    }

    /**
     * returns the protocol this context was created with
     *
     * @return this context's path
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * returns a mutable Map, which can be used to pass
     * configuration and other data to Filter modules
     * and to the context's exchange handler.
     * <p>
     * Every attribute stored in this Map will be visible to
     * every HttpExchange processed by this context
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public List<Filter> getFilters() {
        return ufilters;
    }

    public List<Filter> getSystemFilters() {
        return sfilters;
    }

    public Authenticator setAuthenticator(Authenticator auth) {
        Authenticator old = authenticator;
        authenticator = auth;
        //authfilter.setAuthenticator (auth);
        return old;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    System.Logger getLogger() {
        return server.getLogger();
    }
}

