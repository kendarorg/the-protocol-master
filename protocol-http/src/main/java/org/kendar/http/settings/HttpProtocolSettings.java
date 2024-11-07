package org.kendar.http.settings;

import org.kendar.settings.ProtocolSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpProtocolSettings extends ProtocolSettings {
    private int http;
    private int https;
    private int proxy;
    private String apis;
    private HttpSSLSettings ssl;

    public List<HttpRewriteSettings> getRewrites() {
        return rewrites;
    }

    public void setRewrites(List<HttpRewriteSettings> rewrites) {
        this.rewrites = rewrites;
    }

    private List<HttpRewriteSettings> rewrites = new ArrayList<>();

    public void setHttp(int http) {
        this.http = http;
    }

    public int getHttp() {
        return http;
    }

    public void setHttps(int https) {
        this.https = https;
    }

    public int getHttps() {
        return https;
    }

    public void setProxy(int proxy) {
        this.proxy = proxy;
    }

    public int getProxy() {
        return proxy;
    }

    public void setApis(String apis) {
        this.apis = apis;
    }

    public String getApis() {
        return apis;
    }

    public void setSSL(HttpSSLSettings ssl) {
        this.ssl = ssl;
    }

    public HttpSSLSettings getSSL() {
        return ssl;
    }
}
