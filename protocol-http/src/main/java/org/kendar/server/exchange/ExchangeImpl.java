package org.kendar.server.exchange;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.server.events.WriteFinishedEvent;
import org.kendar.server.streams.*;
import org.kendar.server.utils.ServerImpl;
import org.kendar.server.utils.UnmodifiableHeaders;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings({"resource", "IfStatementWithIdenticalBranches"})
public class ExchangeImpl {

    /* for formatting the Date: header */
    private static final DateTimeFormatter FORMATTER;
    private static final String HEAD = "HEAD";

    static {
        String pattern = "EEE, dd MMM yyyy HH:mm:ss zzz";
        FORMATTER = DateTimeFormatter.ofPattern(pattern, Locale.US)
                .withZone(ZoneId.of("GMT"));
    }

    public boolean writefinished;
    /* close the underlying connection when this exchange finished */
    public boolean close;
    public boolean http10 = false;
    Headers reqHdrs, rspHdrs;
    ExchangeRequest req;
    String method;
    URI uri;
    HttpConnection connection;
    long reqContentLen;
    long rspContentLen;
    /* raw streams which access the socket directly */
    InputStream ris;
    OutputStream ros;
    boolean closed;
    /* streams which take care of the HTTP protocol framing
     * and are passed up to higher layers
     */
    InputStream uis;
    OutputStream uos;
    LeftOverInputStream uis_orig; // uis may have be a user supplied wrapper
    PlaceholderOutputStream uos_orig;

    boolean sentHeaders; /* true after response headers sent */
    Map<String, Object> attributes;
    int rcode = -1;
    HttpPrincipal principal;
    ServerImpl server;
    private byte[] rspbuf = new byte[128]; // used by bytes()

    public ExchangeImpl(
            String m, URI u, ExchangeRequest req, long len, HttpConnection connection
    ) throws IOException {
        this.req = req;
        this.reqHdrs = new UnmodifiableHeaders(req.headers());
        this.rspHdrs = new Headers();
        this.method = m;
        this.uri = u;
        this.connection = connection;
        this.reqContentLen = len;
        /* ros only used for headers, body written directly to stream */
        this.ros = req.outputStream();
        this.ris = req.inputStream();
        server = getServerImpl();
        server.startExchange();
    }

    static ExchangeImpl get(HttpExchange t) {
        if (t instanceof HttpExchangeImpl) {
            return ((HttpExchangeImpl) t).getExchangeImpl();
        } else {
            assert t instanceof HttpsExchangeImpl;
            return ((HttpsExchangeImpl) t).getExchangeImpl();
        }
    }

    public Headers getRequestHeaders() {
        return reqHdrs;
    }

    public Headers getResponseHeaders() {
        return rspHdrs;
    }

    public URI getRequestURI() {
        return uri;
    }

    public String getRequestMethod() {
        return method;
    }

    public HttpContextImpl getHttpContext() {
        return connection.getHttpContext();
    }

    private boolean isHeadRequest() {
        return HEAD.equals(getRequestMethod());
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        /* close the underlying connection if,
         * a) the streams not set up yet, no response can be sent, or
         * b) if the wrapper output stream is not set up, or
         * c) if the close of the input/outpu stream fails
         */
        try {
            if (uis_orig == null || uos == null) {
                connection.close();
                return;
            }
            if (!uos_orig.isWrapped()) {
                connection.close();
                return;
            }
            if (!uis_orig.isClosed()) {
                uis_orig.close();
            }
            uos.close();
        } catch (IOException e) {
            connection.close();
        }
    }

    public InputStream getRequestBody() {
        if (uis != null) {
            return uis;
        }
        if (reqContentLen == -1L) {
            uis_orig = new ChunkedInputStream(this, ris);
            uis = uis_orig;
        } else {
            uis_orig = new FixedLengthInputStream(this, ris, reqContentLen);
            uis = uis_orig;
        }
        return uis;
    }

    public LeftOverInputStream getOriginalInputStream() {
        return uis_orig;
    }

    public int getResponseCode() {
        return rcode;
    }

    public OutputStream getResponseBody() {
        /* TODO. Change spec to remove restriction below. Filters
         * cannot work with this restriction
         *
         * if (!sentHeaders) {
         *    throw new IllegalStateException ("headers not sent");
         * }
         */
        if (uos == null) {
            uos_orig = new PlaceholderOutputStream(null);
            uos = uos_orig;
        }
        return uos;
    }

    /* returns the place holder stream, which is the stream
     * returned from the 1st call to getResponseBody()
     * The "real" ouputstream is then placed inside this
     */
    PlaceholderOutputStream getPlaceholderResponseBody() {
        getResponseBody();
        return uos_orig;
    }

    @SuppressWarnings("SimplifyOptionalCallChains")
    public void sendResponseHeaders(int rCode, long contentLen)
            throws IOException {
        final System.Logger log = server.getLogger();
        if (sentHeaders) {
            throw new IOException("headers already sent");
        }
        this.rcode = rCode;
        String statusLine = "HTTP/1.1 " + rCode + Code.msg(rCode) + "\r\n";
        OutputStream tmpout = new BufferedOutputStream(ros);
        PlaceholderOutputStream o = getPlaceholderResponseBody();
        tmpout.write(bytes(statusLine, 0), 0, statusLine.length());
        boolean noContentToSend = false; // assume there is content
        boolean noContentLengthHeader = false; // must not send Content-length is set
        rspHdrs.set("Date", FORMATTER.format(Instant.now()));

        /* check for response type that is not allowed to send a body */

        if ((rCode >= 100 && rCode < 200) /* informational */
                || (rCode == 204)           /* no content */
                || (rCode == 304))          /* not modified */ {
            if (contentLen != -1) {
                String msg = "sendResponseHeaders: rCode = " + rCode
                        + ": forcing contentLen = -1";
                log.log(System.Logger.Level.WARNING, msg);
            }
            contentLen = -1;
            noContentLengthHeader = (rCode != 304);
        }

        if (isHeadRequest() || rCode == 304) {
            /* HEAD requests or 304 responses should not set a content length by passing it
             * through this API, but should instead manually set the required
             * headers.*/
            if (contentLen >= 0) {
                String msg =
                        "sendResponseHeaders: being invoked with a content length for a HEAD request";
                log.log(System.Logger.Level.WARNING, msg);
            }
            noContentToSend = true;
            contentLen = 0;
        } else { /* not a HEAD request or 304 response */
            if (contentLen == 0) {
                if (http10) {
                    o.setWrappedStream(new UndefLengthOutputStream(this, ros));
                    close = true;
                } else {
                    rspHdrs.set(ConstantsHeader.TRANSFER_ENCODING, "chunked");
                    o.setWrappedStream(new ChunkedOutputStream(this, ros));
                }
            } else {
                if (contentLen == -1) {
                    noContentToSend = true;
                    contentLen = 0;
                }
                if (!noContentLengthHeader) {
                    rspHdrs.set(ConstantsHeader.CONTENT_LENGTH, Long.toString(contentLen));
                }
                o.setWrappedStream(new FixedLengthOutputStream(this, ros, contentLen));
            }
        }

        // A custom handler can request that the connection be
        // closed after the exchange by supplying Connection: close
        // to the response header. Nothing to do if the exchange is
        // already set up to be closed.
        if (!close) {
            Stream<String> conheader =
                    Optional.ofNullable(rspHdrs.get("Connection"))
                            .map(List::stream).orElse(Stream.empty());
            if (conheader.anyMatch("close"::equalsIgnoreCase)) {
                log.log(System.Logger.Level.DEBUG, "Connection: close requested by handler");
                close = true;
            }
        }

        write(rspHdrs, tmpout);
        this.rspContentLen = contentLen;
        tmpout.flush();
        tmpout = null;
        sentHeaders = true;
        log.log(System.Logger.Level.TRACE, "Sent headers: noContentToSend=" + noContentToSend);
        if (noContentToSend) {
            WriteFinishedEvent e = new WriteFinishedEvent(this);
            server.addEvent(e);
            closed = true;
        }
        server.logReply(rCode, req.requestLine(), null);
    }

    void write(Headers map, OutputStream os) throws IOException {
        Set<Map.Entry<String, List<String>>> entries = map.entrySet();
        for (Map.Entry<String, List<String>> entry : entries) {
            String key = entry.getKey();
            byte[] buf;
            List<String> values = entry.getValue();
            for (String val : values) {
                int i = key.length();
                buf = bytes(key, 2);
                buf[i++] = ':';
                buf[i++] = ' ';
                os.write(buf, 0, i);
                buf = bytes(val, 2);
                i = val.length();
                buf[i++] = '\r';
                buf[i++] = '\n';
                os.write(buf, 0, i);
            }
        }
        os.write('\r');
        os.write('\n');
    }

    /**
     * convert string to byte[], using rspbuf
     * Make sure that at least "extra" bytes are free at end
     * of rspbuf. Reallocate rspbuf if not big enough.
     * caller must check return value to see if rspbuf moved
     */
    private byte[] bytes(String s, int extra) {
        int slen = s.length();
        if (slen + extra > rspbuf.length) {
            int diff = slen + extra - rspbuf.length;
            rspbuf = new byte[2 * (rspbuf.length + diff)];
        }
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            rspbuf[i] = (byte) c[i];
        }
        return rspbuf;
    }

    public InetSocketAddress getRemoteAddress() {
        Socket s = connection.getChannel().socket();
        InetAddress ia = s.getInetAddress();
        int port = s.getPort();
        return new InetSocketAddress(ia, port);
    }

    public InetSocketAddress getLocalAddress() {
        Socket s = connection.getChannel().socket();
        InetAddress ia = s.getLocalAddress();
        int port = s.getLocalPort();
        return new InetSocketAddress(ia, port);
    }

    public String getProtocol() {
        String reqline = req.requestLine();
        int index = reqline.lastIndexOf(' ');
        return reqline.substring(index + 1);
    }

    public SSLSession getSSLSession() {
        SSLEngine e = connection.getSSLEngine();
        if (e == null) {
            return null;
        }
        return e.getSession();
    }

    public Object getAttribute(String name) {
        if (name == null) {
            throw new NullPointerException("null name parameter");
        }
        if (attributes == null) {
            attributes = getHttpContext().getAttributes();
        }
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("null name parameter");
        }
        if (attributes == null) {
            attributes = getHttpContext().getAttributes();
        }
        attributes.put(name, value);
    }

    public void setStreams(InputStream i, OutputStream o) {
        assert uis != null;
        if (i != null) {
            uis = i;
        }
        if (o != null) {
            uos = o;
        }
    }

    /**
     * PP
     */
    public HttpConnection getConnection() {
        return connection;
    }

    public ServerImpl getServerImpl() {
        return getHttpContext().getServerImpl();
    }

    public HttpPrincipal getPrincipal() {
        return principal;
    }

    void setPrincipal(HttpPrincipal principal) {
        this.principal = principal;
    }
}


