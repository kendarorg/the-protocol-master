package org.kendar.http;

import org.kendar.di.annotations.TpmService;
import org.kendar.http.settings.HttpSSLSettings;
import org.kendar.settings.ProtocolSettings;

@TpmService(tags = "http")
public class HttpProtocolSettings extends ProtocolSettings {
    private int http = 80;
    private int https = 443;
    private int proxy = 9999;
    private HttpSSLSettings ssl = new HttpSSLSettings();

    public HttpProtocolSettings() {
        setProtocol("http");
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
