package org.kendar.mssql;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.TestInfo;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.mssql.plugins.*;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.*;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.Server;
import org.kendar.tests.testcontainer.images.MsSqlServerImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.kendar.utils.parser.SimpleParser;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MssqlBasicTest {

    protected static final int FAKE_PORT = 1435;
    private static final ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();
    protected static MsSqlServerImage mssqlContainer;
    protected static Server protocolServer;
    protected static MssqlProtocol baseProtocol;
    protected static ProtocolPluginDescriptor errorPlugin;
    private static ProtocolPluginDescriptor latencyPlugin;
    private static ProtocolPluginDescriptor forwarderPlugin;

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        mssqlContainer = new MsSqlServerImage();
        mssqlContainer
                .withNetwork(network)
                .start();
    }

    public static void beforeEachBase(TestInfo testInfo) {
        beforeEachBaseSSL(testInfo, false);
    }

    public static void beforeEachBaseSSL(TestInfo testInfo, boolean ssl) {
        baseProtocol = new MssqlProtocol(FAKE_PORT);
        var proxy = new MssqlProxy("com.microsoft.sqlserver.jdbc.SQLServerDriver",
                mssqlContainer.getJdbcUrl(), null,
                mssqlContainer.getUserId(), mssqlContainer.getPassword());
        StorageRepository storage = new NullStorageRepository();
        if (testInfo != null && testInfo.getTestClass().isPresent() &&
                testInfo.getTestMethod().isPresent()) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();
            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
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
        var mapper = new JsonMapper();
        storage.initialize();
        var gs = new GlobalSettings();
        errorPlugin = new MssqlNetErrorPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new NetworkErrorPluginSettings().withPercentAction(100));
        latencyPlugin = new MssqlLatencyPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new LatencyPluginSettings().withMinMax(500, 1000).withPercentAction(100));
        forwarderPlugin = new MssqlForwardPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicForwardPluginSettings());

        var pl = new MssqlRecordPlugin(mapper, storage, new MultiTemplateEngine(), new SimpleParser())
                .initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());

        var pl1 = new MssqlMockPlugin(mapper, storage, new MultiTemplateEngine());
        var global = new GlobalSettings();
        var mockPluginSettings = new BasicMockPluginSettings();
        pl1.initialize(global, new JdbcProtocolSettings(), mockPluginSettings);
        var rep = new MssqlReportPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        rep.setActive(true);
        forwarderPlugin.setActive(true);
        proxy.setPluginHandlers(List.of(pl, pl1, rep, errorPlugin, latencyPlugin, forwarderPlugin));

        pl.setActive(true);
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        baseProtocol.setProxy(proxy);
        var mssqlSettings = (MssqlProtocolSettings) baseProtocol.getSettings();
        mssqlSettings.setUseTls(ssl);
        baseProtocol.initialize();
        protocolServer = new NettyServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {

        try {
            EventsQueue.unregister("recorder", ReportDataEvent.class);
            events.clear();
            protocolServer.stop();

            Sleeper.sleep(5000, () -> !protocolServer.isRunning());
        } catch (Exception ex) {

        }
    }

    public static void afterClassBase() throws Exception {
        mssqlContainer.close();
    }

    protected static Connection getProxyConnection() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        c = DriverManager
                .getConnection(String.format(
                                "jdbc:sqlserver://127.0.0.1:%d;encrypt=false;trustServerCertificate=true",
                                FAKE_PORT),
                        mssqlContainer.getUserId(), mssqlContainer.getPassword());
        assertNotNull(c);
        return c;
    }

    protected static Connection getProxyConnectionSsl() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        c = DriverManager
                .getConnection(String.format(
                                "jdbc:sqlserver://127.0.0.1:%d;encrypt=true;trustServerCertificate=true",
                                FAKE_PORT),
                        mssqlContainer.getUserId(), mssqlContainer.getPassword());
        assertNotNull(c);
        return c;
    }

    protected static Connection getRealConnection() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        c = DriverManager
                .getConnection(mssqlContainer.getJdbcUrl(),
                        mssqlContainer.getUserId(), mssqlContainer.getPassword());
        assertNotNull(c);
        return c;
    }

    public List<ReportDataEvent> getEvents() {
        return events.stream().collect(Collectors.toList());
    }
}
