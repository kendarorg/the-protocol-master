package org.kendar.tests.utils;


import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;

public class HttpChecker {
    int seconds;
    int proxyPort = -1;
    String proxyUrl = null;
    String url;
    private Runnable onError = null;
    private boolean showError = true;

    protected HttpChecker() {

    }

    public static HttpChecker checkForSite(int seconds, String url) {
        var result = new HttpChecker();
        result.seconds = seconds;
        result.url = url;
        return result;
    }

    public HttpChecker noError() {
        this.showError = false;
        return this;
    }

    public HttpChecker withProxy(String proxyUrl, int proxyPort) {
        this.proxyUrl = proxyUrl;
        this.proxyPort = proxyPort;
        return this;
    }

    public HttpChecker onError(Runnable onError) {
        this.onError = onError;
        return this;
    }


    public boolean run() throws Exception {
        LogWriter.info("Testing for %d seconds %s: ", seconds, url);
        var result = TestSleeper.sleepNoException(seconds * 1000L, () -> {
            if (proxyUrl != null) {
                var proxy = new HttpHost("http", proxyUrl, proxyPort);
                var routePlanner = new DefaultProxyRoutePlanner(proxy);
                try (var httpclient = HttpClients.custom().setRoutePlanner(routePlanner).build()) {
                    var httpget = new HttpGet(url);
                    var httpresponse = httpclient.execute(httpget);
                    if (httpresponse.getCode() == 200) {
                        System.out.print("OK\n");
                        return true;
                    }
                } catch (Exception ex) {
                    //NOP
                }
            } else {

                try (var httpclient = HttpClients.createDefault()) {
                    var httpget = new HttpGet(url);
                    var httpresponse = httpclient.execute(httpget);
                    if (httpresponse.getCode() == 200) {
                        System.out.print("OK\n");
                        return true;
                    }
                } catch (Exception ex) {
                    //NOP
                }
            }
            return false;
        });
        if (!result) {
            if (showError) {
                LogWriter.errror("testing " + url);
            }
            if (onError != null) {
                onError.run();
            }
        }
        return result;
    }
}
