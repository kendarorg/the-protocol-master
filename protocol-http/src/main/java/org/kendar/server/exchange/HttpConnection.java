package org.kendar.server.exchange;


import org.kendar.server.utils.SSLStreams;
import org.kendar.server.utils.ServerImpl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * encapsulates all the connection specific state for a HTTP/S connection
 * one of these is hung from the selector attachment and is used to locate
 * everything from that.
 */
public class HttpConnection {

    public SSLStreams sslStreams;
    /* low level stream that sits directly over channel */
    public InputStream raw;
    public OutputStream rawout;
    public SelectionKey selectionKey;
    public long idleStartTime; // absolute time in milli seconds, starting when the connection was marked idle
    public volatile long reqStartedTime; // time when the request was initiated
    public volatile long rspStartedTime; // time we started writing the response
    HttpContextImpl context;
    SSLEngine engine;
    SSLContext sslContext;
    /* high level streams returned to application */
    InputStream i;
    SocketChannel chan;
    String protocol;
    int remaining;
    boolean closed = false;
    System.Logger logger;
    volatile HttpConnection.State state;

    ;
    public HttpConnection() {
    }

    public String toString() {
        String s = null;
        if (chan != null) {
            s = chan.toString();
        }
        return s;
    }

    public void setContext(HttpContextImpl ctx) {
        context = ctx;
    }

    public HttpConnection.State getState() {
        return state;
    }

    public void setState(HttpConnection.State s) {
        state = s;
    }

    public void setParameters(
            InputStream in, OutputStream rawout, SocketChannel chan,
            SSLEngine engine, SSLStreams sslStreams, SSLContext sslContext, String protocol,
            HttpContextImpl context, InputStream raw
    ) {
        this.context = context;
        this.i = in;
        this.rawout = rawout;
        this.raw = raw;
        this.protocol = protocol;
        this.engine = engine;
        this.chan = chan;
        this.sslContext = sslContext;
        this.sslStreams = sslStreams;
        this.logger = context.getLogger();
    }

    public SocketChannel getChannel() {
        return chan;
    }

    public void setChannel(SocketChannel c) {
        chan = c;
    }

    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (logger != null && chan != null) {
            logger.log(System.Logger.Level.TRACE, "Closing connection: " + chan.toString());
        }

        if (!chan.isOpen()) {
            ServerImpl.dprint("Channel already closed");
            return;
        }
        try {
            /* need to ensure temporary selectors are closed */
            if (raw != null) {
                raw.close();
            }
        } catch (IOException e) {
            ServerImpl.dprint(e);
        }
        try {
            if (rawout != null) {
                rawout.close();
            }
        } catch (IOException e) {
            ServerImpl.dprint(e);
        }
        try {
            if (sslStreams != null) {
                sslStreams.close();
            }
        } catch (IOException e) {
            ServerImpl.dprint(e);
        }
        try {
            chan.close();
        } catch (IOException e) {
            ServerImpl.dprint(e);
        }
    }

    int getRemaining() {
        return remaining;
    }

    /* remaining is the number of bytes left on the lowest level inputstream
     * after the exchange is finished
     */
    void setRemaining(int r) {
        remaining = r;
    }

    SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public InputStream getInputStream() {
        return i;
    }

    public OutputStream getRawOutputStream() {
        return rawout;
    }

    String getProtocol() {
        return protocol;
    }

    SSLEngine getSSLEngine() {
        return engine;
    }

    SSLContext getSSLContext() {
        return sslContext;
    }

    public HttpContextImpl getHttpContext() {
        return context;
    }

    public enum State {IDLE, REQUEST, RESPONSE, NEWLY_ACCEPTED}
}