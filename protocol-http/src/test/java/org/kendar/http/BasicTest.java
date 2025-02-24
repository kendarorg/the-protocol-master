package org.kendar.http;


import org.apache.commons.io.FileUtils;
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
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.http.plugins.*;
import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.plugins.settings.RewritePluginSettings;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.TcpServer;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicTest {

    protected static TcpServer protocolServer;
    protected static HttpProtocol baseProtocol;
    static int FAKE_PORT_HTTP = 8087;
    static int FAKE_PORT_HTTPS = 8487;
    static int FAKE_PORT_PROXY = 9999;
    private static SimpleHttpServer simpleServer;
    private static ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();
    protected GlobalSettings globalSettings;
    protected HttpProtocolSettings httpProtocolSettings;

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
                var splitDsp = dsp.split(",");
                if (splitDsp.length > 0) {
                    if (splitDsp[0].contains("DSC_")) {
                        dsp = splitDsp[0];
                    }
                }
                storage = new FileStorageRepository(Path.of("target", "tests", className, method, dsp));
                try {
                    FileUtils.copyDirectory(Path.of("src", "test", "resources", "data").toFile(),
                            Path.of("target", "tests", className, method, dsp).toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                storage = new FileStorageRepository(Path.of("target", "tests", className, method));
                try {
                    FileUtils.copyDirectory(Path.of("src", "test", "resources", "data").toFile(),
                            Path.of("target", "tests", className, method).toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        storage.initialize();
        globalSettings = new GlobalSettings();
        httpProtocolSettings = new HttpProtocolSettings();
        httpProtocolSettings.setProtocol("http");
        httpProtocolSettings.setHttps(FAKE_PORT_HTTPS);
        httpProtocolSettings.setHttp(FAKE_PORT_HTTP);
        httpProtocolSettings.setProxy(FAKE_PORT_PROXY);
        httpProtocolSettings.setProtocolInstanceId("default");
        var rewriteSettings = new RewritePluginSettings();
        httpProtocolSettings.getPlugins().put("rewrite-plugin", rewriteSettings);

        var recordingSettings = new HttpRecordPluginSettings();
        httpProtocolSettings.getPlugins().put("record-plugin", recordingSettings);

        var reportSettings = new PluginSettings();
        reportSettings.setActive(true);
        httpProtocolSettings.getPlugins().put("report-plugin", reportSettings);

        var latencySettings = new HttpLatencyPluginSettings();
        httpProtocolSettings.getPlugins().put("latency-plugin", latencySettings);

        var ratelimit = new HttpRateLimitPluginSettings();
        httpProtocolSettings.getPlugins().put("rate-limit-plugin", ratelimit);

        var replaySettings = new HttpReplayPluginSettings();
        httpProtocolSettings.getPlugins().put("replay-plugin", replaySettings);
        var mockSettings = new BasicMockPluginSettings();
        httpProtocolSettings.getPlugins().put("mock-plugin", mockSettings);
        globalSettings.getProtocols().put("http", httpProtocolSettings);
        var settings = new PluginSettings();
        settings.setActive(true);
        var mapper = new JsonMapper();
        baseProtocol = new HttpProtocol(globalSettings, httpProtocolSettings, new ArrayList<>(List.of(
                new HttpRecordPlugin(mapper, storage).initialize(globalSettings, httpProtocolSettings, recordingSettings),
                new HttpReplayPlugin(mapper, storage).initialize(globalSettings, httpProtocolSettings, replaySettings),
                new HttpErrorPlugin(mapper),
                new HttpReportPlugin(mapper).initialize(globalSettings, httpProtocolSettings, settings),
                new HttpLatencyPlugin(mapper),
                new HttpRateLimitPlugin(mapper, storage),
                new HttpMockPlugin(mapper, storage).initialize(globalSettings, httpProtocolSettings, mockSettings),
                new HttpRewritePlugin(mapper, storage).initialize(globalSettings, httpProtocolSettings, rewriteSettings))));
        baseProtocol.initialize();
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);


        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public List<ReportDataEvent> getEvents() {
        return events.stream().collect(Collectors.toList());
    }

    public void afterEachBase() {
        EventsQueue.unregister("recorder", ReportDataEvent.class);
        events.clear();
        protocolServer.stop();
        Sleeper.sleep(5000, () -> !protocolServer.isRunning());
    }
}
