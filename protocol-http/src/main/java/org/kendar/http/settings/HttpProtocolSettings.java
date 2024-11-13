package org.kendar.http.settings;

import org.kendar.settings.ProtocolSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpProtocolSettings extends ProtocolSettings {
    private int http;
    private int https;
    private int proxy;
    private HttpSSLSettings ssl;
    private List<HttpRewriteSettings> rewrites = new ArrayList<>();

    public List<HttpRewriteSettings> getRewrites() {
        return rewrites;
    }

    public void setRewrites(List<HttpRewriteSettings> rewrites) {
        this.rewrites = rewrites;
    }

    public int getHttp() {
        return http;
    }

    public void setHttp(int http) {
        this.http = http;
    }

    public int getHttps() {
        return https;
    }

    public void setHttps(int https) {
        this.https = https;
    }

    public int getProxy() {
        return proxy;
    }

    public void setProxy(int proxy) {
        this.proxy = proxy;
    }


    public HttpSSLSettings getSSL() {
        return ssl;
    }

    public void setSSL(HttpSSLSettings ssl) {
        this.ssl = ssl;
    }
}
