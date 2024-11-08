package org.kendar.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Created for http://stackoverflow.com/q/16351413/1266906.
 */
public class ProxyServer {
    public final HashSet<String> ignore = new HashSet<>();
    private final int port;
    //    public static void main(String[] args) {
//        (new ProxyServer()).run();
//    }
//
//    public ProxyServer() {
//        super("Server Thread");
//    }
    private final ExecutorService executor = Executors.newCachedThreadPool();
    ServerSocket serverSocket = null;
    private int httpRedirect = -1;
    private int httpsRedirect = -1;
    private Function<String, String> dnsResolver = s -> {
        return s;
    };

    public ProxyServer(int port) {

        this.port = port;
    }

    public ProxyServer withHttpRedirect(int httpRedirect) {
        this.httpRedirect = httpRedirect;
        return this;
    }

    public ProxyServer withHttpsRedirect(int httpsRedirect) {
        this.httpsRedirect = httpsRedirect;
        return this;
    }

    public ProxyServer withDnsResolver(Function<String, String> dnsResolver) {
        this.dnsResolver = dnsResolver;
        return this;
    }

    public ProxyServer ignoringHosts(String... hosts) {
        ignore.addAll(List.of(hosts));
        return this;
    }

    public void terminate() {

        try {
            serverSocket.close();
        } catch (Exception e) {

        }
    }

    public void start() {

        new Thread(()->{
            try (var ss = new ServerSocket(port)) {
                serverSocket = ss;
                Socket socket;
                try {
                    while ((socket = serverSocket.accept()) != null) {
                        var lambdasocket = socket;
                        executor.submit(() -> new ProxyServerHandler(executor, lambdasocket,
                                httpRedirect, httpsRedirect, dnsResolver, ignore).run());
                    }
                } catch (IOException e) {

                }
            } catch (IOException e) {

            }
        }).start();

    }


}
