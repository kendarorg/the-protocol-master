package org.kendar.server.utils;

import com.sun.net.httpserver.*;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.server.config.ServerConfig;
import org.kendar.server.events.Event;
import org.kendar.server.events.WriteFinishedEvent;
import org.kendar.server.exceptions.HttpError;
import org.kendar.server.exchange.*;
import org.kendar.server.streams.LeftOverInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.channels.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class ServerImpl {
    // schedule for the timer task that's responsible for idle connection management
    static final long IDLE_TIMER_TASK_SCHEDULE = ServerConfig.getIdleTimerScheduleMillis();
    static final int MAX_CONNECTIONS = ServerConfig.getMaxConnections();
    final static int MAX_IDLE_CONNECTIONS = ServerConfig.getMaxIdleConnections();
    // schedule for the timer task that's responsible for request/response timeout management
    static final long REQ_RSP_TIMER_SCHEDULE = ServerConfig.getReqRspTimerScheduleMillis();
    static final long MAX_REQ_TIME = getTimeMillis(ServerConfig.getMaxReqTime());
    static final long MAX_RSP_TIME = getTimeMillis(ServerConfig.getMaxRspTime());
    static final boolean reqRspTimeoutEnabled = MAX_REQ_TIME != -1 || MAX_RSP_TIME != -1;
    // the maximum idle duration for a connection which is currently idle but has served
    // some request in the past
    static final long IDLE_INTERVAL = ServerConfig.getIdleIntervalMillis();
    // the maximum idle duration for a newly accepted connection which hasn't yet received
    // the first byte of data on that connection
    static final long NEWLY_ACCEPTED_CONN_IDLE_INTERVAL;
    static final boolean debug = ServerConfig.debugEnabled();

    static {
        // the idle duration of a newly accepted connection is considered to be the least of the
        // configured idle interval and the configured max request time (if any).
        NEWLY_ACCEPTED_CONN_IDLE_INTERVAL = MAX_REQ_TIME > 0
                ? Math.min(IDLE_INTERVAL, MAX_REQ_TIME)
                : IDLE_INTERVAL;
    }

    private final Set<HttpConnection> idleConnections;
    // connections which have been accepted() by the server but which haven't
    // yet sent any byte on the connection yet
    private final Set<HttpConnection> newlyAcceptedConnections;
    private final Set<HttpConnection> allConnections;
    /* following two are used to keep track of the times
     * when a connection/request is first received
     * and when we start to send the response
     */
    private final Set<HttpConnection> reqConnections;
    private final Set<HttpConnection> rspConnections;
    private final Object lolock = new Object();
    private final System.Logger log;
    private final AtomicReference<InternalSSLConfig> internalSSLConfig = new AtomicReference<>();
    private final String protocol;
    private final boolean https;
    private final ContextList contexts;
    private final InetSocketAddress address;
    private final ServerSocketChannel schan;
    private final Selector selector;
    private final SelectionKey listenerKey;
    private final HttpServer wrapper;
    private final Timer timer;
    ServerImpl.Dispatcher dispatcher;
    private Executor executor;
    private List<Event> events;
    private volatile boolean finished = false;
    private volatile boolean terminating = false;
    private boolean bound = false;
    private boolean started = false;
    private Timer timer1;
    private Thread dispatcherThread;
    private int exchangeCount = 0;

    public ServerImpl(
            HttpServer wrapper, String protocol, InetSocketAddress addr, int backlog
    ) throws IOException {

        this.protocol = protocol;
        this.wrapper = wrapper;
        this.log = System.getLogger("com.sun.net.httpserver");
        ServerConfig.checkLegacyProperties(log);
        https = protocol.equalsIgnoreCase("https");
        this.address = addr;
        contexts = new ContextList();
        schan = ServerSocketChannel.open();
        if (addr != null) {
            ServerSocket socket = schan.socket();
            socket.bind(addr, backlog);
            bound = true;
        }
        selector = Selector.open();
        schan.configureBlocking(false);
        listenerKey = schan.register(selector, SelectionKey.OP_ACCEPT);
        dispatcher = new ServerImpl.Dispatcher();
        idleConnections = Collections.synchronizedSet(new HashSet<>());
        allConnections = Collections.synchronizedSet(new HashSet<>());
        reqConnections = Collections.synchronizedSet(new HashSet<>());
        rspConnections = Collections.synchronizedSet(new HashSet<>());
        newlyAcceptedConnections = Collections.synchronizedSet(new HashSet<>());
        timer = new Timer("idle-timeout-task", true);
        timer.schedule(new ServerImpl.IdleTimeoutTask(), IDLE_TIMER_TASK_SCHEDULE, IDLE_TIMER_TASK_SCHEDULE);
        if (reqRspTimeoutEnabled) {
            timer1 = new Timer("req-rsp-timeout-task", true);
            timer1.schedule(new ServerImpl.ReqRspTimeoutTask(), REQ_RSP_TIMER_SCHEDULE, REQ_RSP_TIMER_SCHEDULE);
            log.log(System.Logger.Level.DEBUG, "HttpServer request/response timeout task schedule ms: ",
                    REQ_RSP_TIMER_SCHEDULE);
            log.log(System.Logger.Level.DEBUG, "MAX_REQ_TIME:  " + MAX_REQ_TIME);
            log.log(System.Logger.Level.DEBUG, "MAX_RSP_TIME:  " + MAX_RSP_TIME);
        }
        events = new LinkedList<>();
        log.log(System.Logger.Level.DEBUG, "HttpServer created " + protocol + " " + addr);
    }

    public static synchronized void dprint(String s) {
        if (debug) {
            System.out.println(s);
        }
    }


    @SuppressWarnings("CallToPrintStackTrace")
    public static synchronized void dprint(Exception e) {
        if (debug) {
            //noinspection ThrowablePrintedToSystemOut
            System.out.println(e);
            e.printStackTrace();
        }
    }

    /**
     * Converts and returns the passed {@code secs} as milli seconds. If the passed {@code secs}
     * is negative or zero or if the conversion from seconds to milli seconds results in a negative
     * number, then this method returns -1.
     */
    private static long getTimeMillis(long secs) {
        if (secs <= 0) {
            return -1;
        }
        final long milli = secs * 1000;
        // this handles potential numeric overflow that may have happened during conversion
        return milli > 0 ? milli : -1;
    }

    /*
     * Validates a RFC 7230 header-key.
     */
    static boolean isValidHeaderKey(String token) {
        if (token == null || token.isEmpty()) return false;

        boolean isValidChar;
        char[] chars = token.toCharArray();
        String validSpecialChars = "!#$%&'*+-.^_`|~";
        for (char c : chars) {
            isValidChar = ((c >= 'a') && (c <= 'z')) ||
                    ((c >= 'A') && (c <= 'Z')) ||
                    ((c >= '0') && (c <= '9'));
            if (!isValidChar && validSpecialChars.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }

    public void bind(InetSocketAddress addr, int backlog) throws IOException {
        if (bound) {
            throw new BindException("HttpServer already bound");
        }
        if (addr == null) {
            throw new NullPointerException("null address");
        }
        ServerSocket socket = schan.socket();
        socket.bind(addr, backlog);
        bound = true;
    }

    public void start() {
        if (!bound || started || finished) {
            throw new IllegalStateException("server in wrong state");
        }
        if (executor == null) {
            executor = new ServerImpl.DefaultExecutor();
        }
        dispatcherThread = new Thread(null, dispatcher, "HTTP-Dispatcher", 0, false);
        started = true;
        dispatcherThread.start();
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        if (started) {
            throw new IllegalStateException("server already started");
        }
        this.executor = executor;
    }

    public HttpsConfigurator getHttpsConfigurator() {
        if (this.internalSSLConfig.get() == null) return null;
        return this.internalSSLConfig.get().getHttpsConfig();
    }

    public void setHttpsConfigurator(HttpsConfigurator config) {
        if (config == null) {
            throw new NullPointerException("null HttpsConfigurator");
        }

        this.internalSSLConfig.set(new InternalSSLConfig(config));
    }

    public final boolean isFinishing() {
        return finished;
    }

    public void stop(int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("negative delay parameter");
        }
        terminating = true;
        try {
            schan.close();
        } catch (IOException ignored) {
        }
        selector.wakeup();
        long latest = System.currentTimeMillis() + delay * 1000L;
        while (System.currentTimeMillis() < latest) {
            delay();
            if (finished) {
                break;
            }
        }
        finished = true;
        selector.wakeup();
        synchronized (allConnections) {
            for (HttpConnection c : allConnections) {
                c.close();
            }
        }
        allConnections.clear();
        idleConnections.clear();
        newlyAcceptedConnections.clear();
        timer.cancel();
        if (reqRspTimeoutEnabled) {
            timer1.cancel();
        }
        if (dispatcherThread != null && dispatcherThread != Thread.currentThread()) {
            try {
                dispatcherThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.log(System.Logger.Level.TRACE, "ServerImpl.stop: ", e);
            }
        }
    }

    public synchronized HttpContextImpl createContext(String path, HttpHandler handler) {
        if (handler == null || path == null) {
            throw new NullPointerException("null handler, or path parameter");
        }
        HttpContextImpl context = new HttpContextImpl(protocol, path, handler, this);
        contexts.add(context);
        log.log(System.Logger.Level.DEBUG, "context created: " + path);
        return context;
    }

    public synchronized HttpContextImpl createContext(String path) {
        if (path == null) {
            throw new NullPointerException("null path parameter");
        }
        HttpContextImpl context = new HttpContextImpl(protocol, path, null, this);
        contexts.add(context);
        log.log(System.Logger.Level.DEBUG, "context created: " + path);
        return context;
    }

    /* main server listener task */

    public synchronized void removeContext(String path) throws IllegalArgumentException {
        if (path == null) {
            throw new NullPointerException("null path parameter");
        }
        contexts.remove(protocol, path);
        log.log(System.Logger.Level.DEBUG, "context removed: " + path);
    }

    public synchronized void removeContext(HttpContext context) throws IllegalArgumentException {
        if (!(context instanceof HttpContextImpl)) {
            throw new IllegalArgumentException("wrong HttpContext type");
        }
        contexts.remove((HttpContextImpl) context);
        log.log(System.Logger.Level.DEBUG, "context removed: " + context.getPath());
    }

    public InetSocketAddress getAddress() {
        return AccessController.doPrivileged(
                (PrivilegedAction<InetSocketAddress>) () -> (InetSocketAddress) schan.socket()
                        .getLocalSocketAddress());
    }

    public void addEvent(Event r) {
        synchronized (lolock) {
            events.add(r);
            selector.wakeup();
        }
    }

    public System.Logger getLogger() {
        return log;
    }

    @SuppressWarnings("AssertWithSideEffects")
    private void closeConnection(HttpConnection conn) {
        conn.close();
        allConnections.remove(conn);
        switch (conn.getState()) {
            case REQUEST:
                reqConnections.remove(conn);
                break;
            case RESPONSE:
                rspConnections.remove(conn);
                break;
            case IDLE:
                idleConnections.remove(conn);
                break;
            case NEWLY_ACCEPTED:
                newlyAcceptedConnections.remove(conn);
                break;
        }
        assert !reqConnections.remove(conn);
        assert !rspConnections.remove(conn);
        assert !idleConnections.remove(conn);
        assert !newlyAcceptedConnections.remove(conn);
    }

    /* per exchange task */

    public void logReply(int code, String requestStr, String text) {
        if (!log.isLoggable(System.Logger.Level.DEBUG)) {
            return;
        }
        if (text == null) {
            text = "";
        }
        String r;
        if (requestStr.length() > 80) {
            r = requestStr.substring(0, 80) + "<TRUNCATED>";
        } else {
            r = requestStr;
        }
        String message = r + " [" + code + " " +
                Code.msg(code) + "] (" + text + ")";
        log.log(System.Logger.Level.DEBUG, message);
    }

    void delay() {
        Thread.yield();
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
    }

    public synchronized void startExchange() {
        exchangeCount++;
    }

    synchronized int endExchange() {
        exchangeCount--;
        assert exchangeCount >= 0;
        return exchangeCount;
    }

    public HttpServer getWrapper() {
        return wrapper;
    }

    void requestStarted(HttpConnection c) {
        c.reqStartedTime = System.currentTimeMillis();
        c.setState(HttpConnection.State.REQUEST);
        reqConnections.add(c);
    }

    void markIdle(HttpConnection c) {
        c.idleStartTime = System.currentTimeMillis();
        c.setState(HttpConnection.State.IDLE);
        idleConnections.add(c);
    }

    void markNewlyAccepted(HttpConnection c) {
        c.idleStartTime = System.currentTimeMillis();
        c.setState(HttpConnection.State.NEWLY_ACCEPTED);
        newlyAcceptedConnections.add(c);
    }

    public void requestCompleted(HttpConnection c) {
        HttpConnection.State s = c.getState();
        assert s == HttpConnection.State.REQUEST : "State is not REQUEST (" + s + ")";
        reqConnections.remove(c);
        c.rspStartedTime = System.currentTimeMillis();
        rspConnections.add(c);
        c.setState(HttpConnection.State.RESPONSE);
    }

    // called after response has been sent
    void responseCompleted(HttpConnection c) {
        HttpConnection.State s = c.getState();
        assert s == HttpConnection.State.RESPONSE : "State is not RESPONSE (" + s + ")";
        rspConnections.remove(c);
        c.setState(HttpConnection.State.IDLE);
    }

    // called after a request has been completely read
    // by the server. This stops the timer which would
    // close the connection if the request doesn't arrive
    // quickly enough. It then starts the timer
    // that ensures the client reads the response in a timely
    // fashion.

    private static class DefaultExecutor implements Executor {
        public void execute(Runnable task) {
            task.run();
        }
    }

    static class InternalSSLConfig {
        private final SSLContext sslContext;
        private final HttpsConfigurator httpsConfig;

        public InternalSSLConfig(HttpsConfigurator config) {

            this.httpsConfig = config;
            this.sslContext = config.getSSLContext();
        }

        public SSLContext getSslContext() {
            return sslContext;
        }

        public HttpsConfigurator getHttpsConfig() {
            return httpsConfig;
        }
    }

    /**
     * The Dispatcher is responsible for accepting any connections and then using those connections
     * to processing any incoming requests. A connection is represented as an instance of
     * sun.net.httpserver.HttpConnection.
     * <p>
     * Connection states:
     * An instance of HttpConnection goes through the following states:
     * <p>
     * - NEWLY_ACCEPTED: A connection is marked as newly accepted as soon as the Dispatcher
     * accept()s a connection. A newly accepted connection is added to a newlyAcceptedConnections
     * collection. A newly accepted connection also gets added to the allConnections collection.
     * The newlyAcceptedConnections isn't checked for any size limits, however, if the server is
     * configured with a maximum connection limit, then the elements in the
     * newlyAcceptedConnections will never exceed that configured limit (no explicit size checks
     * are done on the newlyAcceptedConnections collection, since the maximum connection limit
     * applies to connections across different connection states). A connection in NEWLY_ACCEPTED
     * state is considered idle and is eligible for idle connection management.
     * <p>
     * - REQUEST: A connection is marked to be in REQUEST state when the request processing starts
     * on that connection. This typically happens when the first byte of data is received on a
     * NEWLY_ACCEPTED connection or when new data arrives on a connection which was previously
     * in IDLE state. When a connection is in REQUEST state, it implies that the connection is
     * active and thus isn't eligible for idle connection management. If the server is configured
     * with a maximum request timeout, then connections in REQUEST state are eligible
     * for Request/Response timeout management.
     * <p>
     * - RESPONSE: A connection is marked to be in RESPONSE state when the server has finished
     * reading the request. A connection is RESPONSE state is considered active and isn't eligible
     * for idle connection management. If the server is configured with a maximum response timeout,
     * then connections in RESPONSE state are eligible for Request/Response timeout management.
     * <p>
     * - IDLE: A connection is marked as IDLE when a request/response cycle (successfully) completes
     * on that particular connection. Idle connections are held in a idleConnections collection.
     * The idleConnections collection is limited in size and the size is decided by a server
     * configuration. Connections in IDLE state get added to the idleConnections collection only
     * if that collection hasn't reached the configured limit. If a connection has reached IDLE
     * state and there's no more room in the idleConnections collection, then such a connection
     * gets closed. Connections in idleConnections collection are eligible for idle connection
     * management.
     * <p>
     * Idle connection management:
     * A timer task is responsible for closing idle connections. Each connection that is in a state
     * which is eligible for idle timeout management (see above section on connection states)
     * will have a corresponding idle expiration time associated with it. The idle timeout management
     * task will check the expiration time of each such connection against the current time and will
     * close the connection if the current time is either equal to or past the expiration time.
     * <p>
     * Request/Response timeout management:
     * The server can be optionally configured with a maximum request timeout and/or maximum response
     * timeout. If either of these timeouts have been configured, then an additional timer task is
     * run by the server. This timer task is then responsible for closing connections which have
     * been in REQUEST or RESPONSE state for a period of time that exceeds the respective configured
     * timeouts.
     * <p>
     * Maximum connection limit management:
     * The server can be optionally configured with a maximum connection limit. A value of 0 or
     * negative integer is ignored and considered to represent no connection limit. In case of a
     * positive integer value, any newly accepted connections will be first checked against the
     * current count of established connections (held by the allConnections collection) and if the
     * configured limit has reached, then the newly accepted connection will be closed immediately
     * (even before setting its state to NEWLY_ACCEPTED or adding it to the newlyAcceptedConnections
     * collection).
     */
    class Dispatcher implements Runnable {

        final LinkedList<HttpConnection> connsToRegister =
                new LinkedList<>();

        private void handleEvent(Event r) {
            ExchangeImpl t = r.exchange;
            HttpConnection c = t.getConnection();
            try {
                if (r instanceof WriteFinishedEvent) {

                    log.log(System.Logger.Level.TRACE, "Write Finished");
                    int exchanges = endExchange();
                    if (terminating && exchanges == 0) {
                        finished = true;
                    }
                    LeftOverInputStream is = t.getOriginalInputStream();
                    if (!is.isEOF()) {
                        t.close = true;
                        if (c.getState() == HttpConnection.State.REQUEST) {
                            requestCompleted(c);
                        }
                    }
                    responseCompleted(c);
                    if (t.close || idleConnections.size() >= MAX_IDLE_CONNECTIONS) {
                        c.close();
                        allConnections.remove(c);
                    } else {
                        if (is.isDataBuffered()) {
                            /* don't re-enable the interestops, just handle it */
                            requestStarted(c);
                            handle(c.getChannel(), c);
                        } else {
                            connsToRegister.add(c);
                        }
                    }
                }
            } catch (IOException e) {
                log.log(
                        System.Logger.Level.TRACE, "Dispatcher (1)", e
                );
                c.close();
            }
        }

        void reRegister(HttpConnection c) {
            /* re-register with selector */
            try {
                SocketChannel chan = c.getChannel();
                chan.configureBlocking(false);
                SelectionKey key = chan.register(selector, SelectionKey.OP_READ);
                key.attach(c);
                c.selectionKey = key;
                markIdle(c);
            } catch (IOException e) {
                dprint(e);
                log.log(System.Logger.Level.TRACE, "Dispatcher(8)", e);
                c.close();
            }
        }

        public void run() {
            while (!finished) {
                try {
                    List<Event> list = null;
                    synchronized (lolock) {
                        if (!events.isEmpty()) {
                            list = events;
                            events = new LinkedList<>();
                        }
                    }

                    if (list != null) {
                        for (Event r : list) {
                            handleEvent(r);
                        }
                    }

                    for (HttpConnection c : connsToRegister) {
                        reRegister(c);
                    }
                    connsToRegister.clear();

                    selector.select(1000);

                    /* process the selected list now  */
                    Set<SelectionKey> selected = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selected.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.equals(listenerKey)) {
                            if (terminating) {
                                continue;
                            }
                            SocketChannel chan = schan.accept();
                            // optimist there's a channel
                            if (chan != null) {
                                if (MAX_CONNECTIONS > 0 && allConnections.size() >= MAX_CONNECTIONS) {
                                    // we've hit max limit of current open connections, so we go
                                    // ahead and close this connection without processing it
                                    try {
                                        chan.close();
                                    } catch (IOException ignore) {
                                    }
                                    // move on to next selected key
                                    continue;
                                }

                                // Set TCP_NODELAY, if appropriate
                                if (ServerConfig.noDelay()) {
                                    chan.socket().setTcpNoDelay(true);
                                }
                                chan.configureBlocking(false);
                                SelectionKey newkey =
                                        chan.register(selector, SelectionKey.OP_READ);
                                HttpConnection c = new HttpConnection();
                                c.selectionKey = newkey;
                                c.setChannel(chan);
                                newkey.attach(c);
                                markNewlyAccepted(c);
                                allConnections.add(c);
                            }
                        } else {
                            try {
                                if (key.isReadable()) {
                                    SocketChannel chan = (SocketChannel) key.channel();
                                    HttpConnection conn = (HttpConnection) key.attachment();

                                    key.cancel();
                                    chan.configureBlocking(true);
                                    if (newlyAcceptedConnections.remove(conn)
                                            || idleConnections.remove(conn)) {
                                        // was either a newly accepted connection or an idle
                                        // connection. In either case, we mark that the request
                                        // has now started on this connection.
                                        requestStarted(conn);
                                    }
                                    handle(chan, conn);
                                } else {
                                    assert false : "Unexpected non-readable key:" + key;
                                }
                            } catch (CancelledKeyException e) {
                                handleException(key, null);
                            } catch (IOException e) {
                                handleException(key, e);
                            }
                        }
                    }
                    // call the selector just to process the cancelled keys
                    selector.selectNow();
                } catch (IOException e) {
                    log.log(System.Logger.Level.TRACE, "Dispatcher (4)", e);
                } catch (Exception e) {
                    log.log(System.Logger.Level.TRACE, "Dispatcher (7)", e);
                }
            }
            try {
                selector.close();
            } catch (Exception ignored) {
            }
        }

        private void handleException(SelectionKey key, Exception e) {
            HttpConnection conn = (HttpConnection) key.attachment();
            if (e != null) {
                log.log(System.Logger.Level.TRACE, "Dispatcher (2)", e);
            }
            closeConnection(conn);
        }

        public void handle(SocketChannel chan, HttpConnection conn) {
            try {
                ServerImpl.Exchange t = new ServerImpl.Exchange(chan, protocol, conn);
                executor.execute(t);
            } catch (HttpError e1) {
                log.log(System.Logger.Level.TRACE, "Dispatcher (4)", e1);
                closeConnection(conn);
            } catch (IOException e) {
                log.log(System.Logger.Level.TRACE, "Dispatcher (5)", e);
                closeConnection(conn);
            } catch (Throwable e) {
                log.log(System.Logger.Level.TRACE, "Dispatcher (6)", e);
                closeConnection(conn);
            }
        }
    }

    class Exchange implements Runnable {
        final SocketChannel chan;
        final HttpConnection connection;
        final String protocol;
        HttpContextImpl context;
        InputStream rawin;
        OutputStream rawout;
        ExchangeImpl tx;
        HttpContextImpl ctx;
        boolean rejected = false;

        Exchange(SocketChannel chan, String protocol, HttpConnection conn) throws IOException {
            this.chan = chan;
            this.connection = conn;
            this.protocol = protocol;
        }

        public void run() {
            /* context will be null for new connections */
            log.log(System.Logger.Level.TRACE, "exchange started");
            context = connection.getHttpContext();
            boolean newconnection;
            SSLEngine engine = null;
            String requestLine = null;
            SSLStreams sslStreams = null;
            try {
                if (context != null) {
                    this.rawin = connection.getInputStream();
                    this.rawout = connection.getRawOutputStream();
                    newconnection = false;
                } else {
                    /* figure out what kind of connection this is */
                    newconnection = true;
                    if (https) {
                        if (internalSSLConfig.get().getSslContext() == null) {
                            log.log(System.Logger.Level.WARNING,
                                    "SSL connection received. No https context created");
                            throw new HttpError("No SSL context established");
                        }
                        sslStreams = new SSLStreams(ServerImpl.this, internalSSLConfig.get().getSslContext(), chan);
                        rawin = sslStreams.getInputStream();
                        rawout = sslStreams.getOutputStream();
                        engine = sslStreams.getSSLEngine();
                        connection.sslStreams = sslStreams;
                    } else {
                        rawin = new BufferedInputStream(
                                new ExchangeRequest.ReadStream(
                                        ServerImpl.this, chan
                                ));
                        rawout = new ExchangeRequest.WriteStream(
                                ServerImpl.this, chan
                        );
                    }
                    connection.raw = rawin;
                    connection.rawout = rawout;
                }
                ExchangeRequest req = new ExchangeRequest(rawin, rawout);
                requestLine = req.requestLine();
                if (requestLine == null) {
                    /* connection closed */
                    log.log(System.Logger.Level.DEBUG, "no request line: closing");
                    closeConnection(connection);
                    return;
                }
                log.log(System.Logger.Level.DEBUG, "Exchange request line: {0}", requestLine);
                int space = requestLine.indexOf(' ');
                if (space == -1) {
                    reject(Code.HTTP_BAD_REQUEST,
                            requestLine, "Bad request line");
                    return;
                }
                String method = requestLine.substring(0, space);
                int start = space + 1;
                space = requestLine.indexOf(' ', start);
                if (space == -1) {
                    reject(Code.HTTP_BAD_REQUEST,
                            requestLine, "Bad request line");
                    return;
                }
                String uriStr = requestLine.substring(start, space);
                URI uri = new URI(uriStr);
                start = space + 1;
                String version = requestLine.substring(start);
                Headers headers = req.headers();
                /* check key for illegal characters */
                for (var k : headers.keySet()) {
                    if (!isValidHeaderKey(k)) {
                        reject(Code.HTTP_BAD_REQUEST, requestLine,
                                "Header key contains illegal characters");
                        return;
                    }
                }
                /* checks for unsupported combinations of lengths and encodings */
                if (headers.containsKey(ConstantsHeader.CONTENT_LENGTH) &&
                        (headers.containsKey(ConstantsHeader.TRANSFER_ENCODING) || headers.get(ConstantsHeader.CONTENT_LENGTH).size() > 1)) {
                    reject(Code.HTTP_BAD_REQUEST, requestLine,
                            "Conflicting or malformed headers detected");
                    return;
                }
                long clen = 0L;
                String headerValue = null;
                List<String> teValueList = headers.get(ConstantsHeader.TRANSFER_ENCODING);
                if (teValueList != null && !teValueList.isEmpty()) {
                    headerValue = teValueList.get(0);
                }
                if (headerValue != null) {
                    if (headerValue.equalsIgnoreCase("chunked") && teValueList.size() == 1) {
                        clen = -1L;
                    } else {
                        reject(Code.HTTP_NOT_IMPLEMENTED,
                                requestLine, "Unsupported Transfer-Encoding value");
                        return;
                    }
                } else {
                    headerValue = headers.getFirst(ConstantsHeader.CONTENT_LENGTH);
                    if (headerValue != null) {
                        clen = Long.parseLong(headerValue);
                        if (clen < 0) {
                            reject(Code.HTTP_BAD_REQUEST, requestLine,
                                    "Illegal Content-Length value");
                            return;
                        }
                    }
                    if (clen == 0) {
                        requestCompleted(connection);
                    }
                }
                ctx = contexts.findContext(protocol, uri.getPath());
                if (ctx == null) {
                    reject(Code.HTTP_NOT_FOUND,
                            requestLine, "No context found for request");
                    return;
                }
                connection.setContext(ctx);
                if (ctx.getHandler() == null) {
                    reject(Code.HTTP_INTERNAL_ERROR,
                            requestLine, "No handler for context");
                    return;
                }
                tx = new ExchangeImpl(
                        method, uri, req, clen, connection
                );
                String chdr = headers.getFirst("Connection");
                Headers rheaders = tx.getResponseHeaders();

                if (chdr != null && chdr.equalsIgnoreCase("close")) {
                    tx.close = true;
                }
                if (version.equalsIgnoreCase("http/1.0")) {
                    tx.http10 = true;
                    if (chdr == null) {
                        tx.close = true;
                        rheaders.set("Connection", "close");
                    } else if (chdr.equalsIgnoreCase("keep-alive")) {
                        rheaders.set("Connection", "keep-alive");
                        int idleSeconds = (int) (ServerConfig.getIdleIntervalMillis() / 1000);
                        int max = ServerConfig.getMaxIdleConnections();
                        String val = "timeout=" + idleSeconds + ", max=" + max;
                        rheaders.set("Keep-Alive", val);
                    }
                }

                if (newconnection) {
                    SSLContext context = null;
                    if (internalSSLConfig.get() != null) {
                        context = internalSSLConfig.get().getSslContext();
                    }
                    connection.setParameters(
                            rawin, rawout, chan, engine, sslStreams,
                            context, protocol, ctx, rawin
                    );
                }
                /* check if client sent an Expect 100 Continue.
                 * In that case, need to send an interim response.
                 * In future API may be modified to allow app to
                 * be involved in this process.
                 */
                String exp = headers.getFirst("Expect");
                if (exp != null && exp.equalsIgnoreCase("100-continue")) {
                    logReply(100, requestLine, null);
                    sendReply(
                            Code.HTTP_CONTINUE, false, null
                    );
                }
                /* uf is the list of filters seen/set by the user.
                 * sf is the list of filters established internally
                 * and which are not visible to the user. uc and sc
                 * are the corresponding Filter.Chains.
                 * They are linked together by a LinkHandler
                 * so that they can both be invoked in one call.
                 */
                final List<Filter> sf = ctx.getSystemFilters();
                final List<Filter> uf = ctx.getFilters();

                final Filter.Chain sc = new Filter.Chain(sf, ctx.getHandler());
                final Filter.Chain uc = new Filter.Chain(uf, new ServerImpl.Exchange.LinkHandler(sc));

                /* set up the two stream references */
                tx.getRequestBody();
                tx.getResponseBody();
                if (https) {
                    uc.doFilter(new HttpsExchangeImpl(tx));
                } else {
                    uc.doFilter(new HttpExchangeImpl(tx));
                }

            } catch (IOException e1) {
                log.log(System.Logger.Level.TRACE, "ServerImpl.Exchange (1)", e1);
                closeConnection(connection);
            } catch (NumberFormatException e2) {
                log.log(System.Logger.Level.TRACE, "ServerImpl.Exchange (2)", e2);
                reject(Code.HTTP_BAD_REQUEST,
                        requestLine, "NumberFormatException thrown");
            } catch (URISyntaxException e3) {
                log.log(System.Logger.Level.TRACE, "ServerImpl.Exchange (3)", e3);
                reject(Code.HTTP_BAD_REQUEST,
                        requestLine, "URISyntaxException thrown");
            } catch (Exception e4) {
                log.log(System.Logger.Level.TRACE, "ServerImpl.Exchange (4)", e4);
                closeConnection(connection);
            } catch (Throwable t) {
                log.log(System.Logger.Level.TRACE, "ServerImpl.Exchange (5)", t);
                throw t;
            }
        }

        /* used to link to 2 or more Filter.Chains together */

        void reject(int code, String requestStr, String message) {
            rejected = true;
            logReply(code, requestStr, message);
            sendReply(
                    code, false, "<h1>" + code + Code.msg(code) + "</h1>" + message
            );
            closeConnection(connection);
        }

        void sendReply(
                int code, boolean closeNow, String text) {
            try {
                StringBuilder builder = new StringBuilder(512);
                builder.append("HTTP/1.1 ")
                        .append(code).append(Code.msg(code)).append("\r\n");

                if (text != null && !text.isEmpty()) {
                    builder.append(ConstantsHeader.CONTENT_LENGTH + ": ")
                            .append(text.length()).append("\r\n")
                            .append(ConstantsHeader.CONTENT_TYPE + ": text/html\r\n");
                } else {
                    builder.append(ConstantsHeader.CONTENT_LENGTH + ": 0\r\n");
                    text = "";
                }
                if (closeNow) {
                    builder.append("Connection: close\r\n");
                }
                builder.append("\r\n").append(text);
                String s = builder.toString();
                byte[] b = s.getBytes("ISO8859_1");
                rawout.write(b);
                rawout.flush();
                if (closeNow) {
                    closeConnection(connection);
                }
            } catch (IOException e) {
                log.log(System.Logger.Level.TRACE, "ServerImpl.sendReply", e);
                closeConnection(connection);
            }
        }

        static class LinkHandler implements HttpHandler {
            final Filter.Chain nextChain;

            LinkHandler(Filter.Chain nextChain) {
                this.nextChain = nextChain;
            }

            public void handle(HttpExchange exchange) throws IOException {
                nextChain.doFilter(exchange);
            }
        }

    }

    /**
     * Responsible for closing connections that have been idle.
     * TimerTask run every CLOCK_TICK ms
     */
    class IdleTimeoutTask extends TimerTask {
        public void run() {
            LinkedList<HttpConnection> toClose = new LinkedList<>();
            final long currentTime = System.currentTimeMillis();
            synchronized (idleConnections) {
                final Iterator<HttpConnection> it = idleConnections.iterator();
                while (it.hasNext()) {
                    final HttpConnection c = it.next();
                    if (currentTime - c.idleStartTime >= IDLE_INTERVAL) {
                        toClose.add(c);
                        it.remove();
                    }
                }
            }
            // if any newly accepted connection has been idle (i.e. no byte has been sent on that
            // connection during the configured idle timeout period) then close it as well
            synchronized (newlyAcceptedConnections) {
                final Iterator<HttpConnection> it = newlyAcceptedConnections.iterator();
                while (it.hasNext()) {
                    final HttpConnection c = it.next();
                    if (currentTime - c.idleStartTime >= NEWLY_ACCEPTED_CONN_IDLE_INTERVAL) {
                        toClose.add(c);
                        it.remove();
                    }
                }
            }
            for (HttpConnection c : toClose) {
                allConnections.remove(c);
                c.close();
                if (log.isLoggable(System.Logger.Level.TRACE)) {
                    log.log(System.Logger.Level.TRACE, "Closed idle connection " + c);
                }
            }
        }
    }

    /**
     * Responsible for closing connections which have timed out while in REQUEST or RESPONSE state
     */
    class ReqRspTimeoutTask extends TimerTask {

        // runs every TIMER_MILLIS
        public void run() {
            LinkedList<HttpConnection> toClose = new LinkedList<>();
            final long currentTime = System.currentTimeMillis();
            synchronized (reqConnections) {
                if (MAX_REQ_TIME != -1) {
                    for (HttpConnection c : reqConnections) {
                        if (currentTime - c.reqStartedTime >= MAX_REQ_TIME) {
                            toClose.add(c);
                        }
                    }
                    for (HttpConnection c : toClose) {
                        log.log(System.Logger.Level.DEBUG, "closing: no request: " + c);
                        reqConnections.remove(c);
                        allConnections.remove(c);
                        c.close();
                    }
                }
            }
            toClose = new LinkedList<>();
            synchronized (rspConnections) {
                if (MAX_RSP_TIME != -1) {
                    for (HttpConnection c : rspConnections) {
                        if (currentTime - c.rspStartedTime >= MAX_RSP_TIME) {
                            toClose.add(c);
                        }
                    }
                    for (HttpConnection c : toClose) {
                        log.log(System.Logger.Level.DEBUG, "closing: no response: " + c);
                        rspConnections.remove(c);
                        allConnections.remove(c);
                        c.close();
                    }
                }
            }
        }
    }
}
