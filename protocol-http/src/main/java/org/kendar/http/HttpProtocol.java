package org.kendar.http;

import org.kendar.http.data.RequestResponseBuilderImpl;
import org.kendar.storage.BaseStorage;
import website.magyar.mitm.proxy.ProxyServer;

public class HttpProtocol  {
    ProxyServer proxyServer = null;
    private int port= 9092;
    private BaseStorage storage;

    public HttpProtocol(int port, BaseStorage storage) {
        this.port = port;
        this.storage = storage;
    }
    protected void start() {
        try {
            proxyServer = new ProxyServer(port);
            var interceptor = new TpmInterceptor(new RequestResponseBuilderImpl());
            proxyServer.start(30000);
            proxyServer.setResponseVolatile(true);
            proxyServer.setCaptureContent(true);
            proxyServer.setCaptureBinaryContent(true);
            proxyServer.addRequestInterceptor(interceptor);
            proxyServer.addResponseInterceptor(interceptor);
            proxyServer.getPort();
            ProxyServer.setShouldKeepSslConnectionAlive(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        var p = new HttpProtocol(9092,null);
        p.start();
    }
}
