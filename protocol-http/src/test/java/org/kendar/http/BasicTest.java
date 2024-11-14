package org.kendar.http;


import org.junit.jupiter.api.TestInfo;
import org.kendar.http.plugins.*;
import org.kendar.http.settings.HttpProtocolSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.List;

public class BasicTest {

    protected static TcpServer protocolServer;
    protected static HttpProtocol baseProtocol;
    static int FAKE_PORT_HTTP = 8087;
    static int FAKE_PORT_HTTPS = 8487;
    static int FAKE_PORT_PROXY = 9999;
    private static SimpleHttpServer simpleServer;

    public static void beforeClassBase() throws Exception {
        simpleServer = new SimpleHttpServer();
        simpleServer.start(8456);

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
        var recordingPlugin = new HttpRecordPluginSettings();
        httpProtocolSettings.getPlugins().put("record-plugin", recordingPlugin);
        var replayPlugin = new HttpReplayPluginSettings();
        httpProtocolSettings.getPlugins().put("replay-plugin", replayPlugin);
        globalSettings.getProtocols().put("http", httpProtocolSettings);
        globalSettings.putService("storage", storage);
        baseProtocol = new HttpProtocol(globalSettings, httpProtocolSettings, List.of(
                new HttpRecordingPlugin().withStorage(storage),
                new HttpReplayingPlugin().withStorage(storage),
                new HttpErrorPlugin()));
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
