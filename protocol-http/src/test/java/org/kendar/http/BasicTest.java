package org.kendar.http;


import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.TestInfo;
import org.kendar.http.plugins.*;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.plugins.RewritePluginSettings;
import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicTest {

    protected static TcpServer protocolServer;
    protected static HttpProtocol baseProtocol;
    static int FAKE_PORT_HTTP = 8087;
    static int FAKE_PORT_HTTPS = 8487;
    static int FAKE_PORT_PROXY = 9999;
    private static SimpleHttpServer simpleServer;

    public static void beforeClassBase() throws Exception {
        java.util.logging.Logger.getLogger("org.apache.http.client").setLevel(Level.OFF);
        simpleServer = new SimpleHttpServer();
        simpleServer.start(8456);

    }

    protected static CloseableHttpClient createHttpClient(HttpClientBuilder custom) {
        var proxy = new HttpHost("localhost", FAKE_PORT_PROXY, "http");
        var routePlanner = new DefaultProxyRoutePlanner(proxy);
        var httpclient = custom.setRoutePlanner(routePlanner).build();
        return httpclient;
    }

    protected static String getContentString(HttpResponse httpresponse) {
        try {
            var sc = new Scanner(httpresponse.getEntity().getContent());

            //Printing the status line
            assertEquals("HTTP/1.1 200 OK", httpresponse.getStatusLine().toString());
            var content = "";
            while (sc.hasNext()) {
                content += sc.nextLine();
            }
            return content;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static HttpRequestBase withQuery(HttpRequestBase req, Map<String, String> queryParms) {
        try {
            var uriBuilder = new URIBuilder(req.getURI());
            for (var q : queryParms.entrySet()) {
                uriBuilder.addParameter(q.getKey(), q.getValue());
            }
            req.setURI(uriBuilder.build());
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static CloseableHttpClient createHttpsHttpClient() throws Exception {
        final var sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        var httpclient = createHttpClient(HttpClients.custom()
                .setSSLContext(sslContext)
                .setConnectionManager(
                        new PoolingHttpClientConnectionManager(
                                RegistryBuilder.<ConnectionSocketFactory>create()
                                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                                        .register("https", new SSLConnectionSocketFactory(sslContext,
                                                NoopHostnameVerifier.INSTANCE))
                                        .build()
                        )));
        return httpclient;
    }

    public static void afterClassBase() throws Exception {
        simpleServer.stop();
    }

    public void beforeEachBase(TestInfo testInfo) {


        StorageRepository storage = new NullStorageRepository();
        if (testInfo != null && testInfo.getTestClass().isPresent() &&
                testInfo.getTestMethod().isPresent()) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();

            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                var splittedDsp = dsp.split(",");
                if (splittedDsp.length > 0) {
                    if (splittedDsp[0].contains("DSC_")) {
                        dsp = splittedDsp[0];
                    }
                }
                storage = new FileStorageRepository(Path.of("target", "tests", className, method, dsp));
            } else {
                storage = new FileStorageRepository(Path.of("target", "tests", className, method));
            }
        }
        storage.initialize();
        var globalSettings = new GlobalSettings();
        var httpProtocolSettings = new HttpProtocolSettings();
        httpProtocolSettings.setProtocol("http");
        httpProtocolSettings.setHttps(FAKE_PORT_HTTPS);
        httpProtocolSettings.setHttp(FAKE_PORT_HTTP);
        httpProtocolSettings.setProxy(FAKE_PORT_PROXY);
        httpProtocolSettings.setProtocolInstanceId("default");
        var rewriteSettings = new RewritePluginSettings();
        rewriteSettings.setRewritesFile(Path.of("src", "test", "resources", "rewrite.json").toAbsolutePath().toString());
        httpProtocolSettings.getPlugins().put("replay-plugin", rewriteSettings);

        var recordingSettings = new HttpRecordPluginSettings();
        httpProtocolSettings.getPlugins().put("record-plugin", recordingSettings);
        var replaySettings = new HttpReplayPluginSettings();
        httpProtocolSettings.getPlugins().put("replay-plugin", replaySettings);
        var mockSettings = new BasicMockPluginSettings();
        mockSettings.setDataDir(Path.of("src", "test", "resources", "mock").toAbsolutePath().toString());
        httpProtocolSettings.getPlugins().put("mock-plugin", mockSettings);
        globalSettings.getProtocols().put("http", httpProtocolSettings);
        globalSettings.putService("storage", storage);
        baseProtocol = new HttpProtocol(globalSettings, httpProtocolSettings, List.of(
                new HttpRecordingPlugin(),
                new HttpReplayingPlugin().withStorage(storage),
                new HttpErrorPlugin(),
                new HttpMockPlugin(),
                new HttpRewritePlugin()));
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);


        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public void afterEachBase() {

        protocolServer.stop();
        Sleeper.sleep(5000, () -> !protocolServer.isRunning());
    }
}
