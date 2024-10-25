package org.kendar.http;

import org.kendar.http.data.RequestResponseBuilderImpl;
import org.kendar.http.rewrite.TpmProxyServer;
import org.kendar.storage.BaseStorage;

public class HttpProtocol  {
    TpmProxyServer tpmProxyServer = null;
    private int port= 9092;
    private BaseStorage storage;

    public HttpProtocol(int port, BaseStorage storage) {
        this.port = port;
        this.storage = storage;
    }
    protected void start() {
        try {
            tpmProxyServer = new TpmProxyServer(port);
            var interceptor = new TpmInterceptor(new RequestResponseBuilderImpl());
            tpmProxyServer.addResponderInterceptor(interceptor);
            tpmProxyServer.start(30000);
            tpmProxyServer.setResponseVolatile(true);
            tpmProxyServer.setCaptureContent(true);
            tpmProxyServer.setCaptureBinaryContent(true);
            tpmProxyServer.addRequestInterceptor(interceptor);
            tpmProxyServer.addResponseInterceptor(interceptor);
            tpmProxyServer.getPort();
            TpmProxyServer.setShouldKeepSslConnectionAlive(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        var p = new HttpProtocol(9092,null);
        p.start();
    }
}
