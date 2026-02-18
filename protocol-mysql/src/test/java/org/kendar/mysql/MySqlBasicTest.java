package org.kendar.mysql;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.TestInfo;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.mysql.plugins.*;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.tcpserver.Server;
import org.kendar.tcpserver.TcpServer;
import org.kendar.tests.testcontainer.images.MysqlImage;
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

public class MySqlBasicTest {

    protected static final int FAKE_PORT = 3310;
    protected static MysqlImage mysqlContainer;
    protected static Server protocolServer;
    protected static MySQLProtocol baseProtocol;
    protected static ProtocolPluginDescriptor errorPlugin;
    private static final ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();
    private static ProtocolPluginDescriptor latencyPlugin;

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        mysqlContainer = new MysqlImage();
        mysqlContainer
                .withNetwork(network)
                .start();


    }

    public static void beforeEachBase(TestInfo testInfo) {
        beforeEachBaseSSL(testInfo, false);
    }

    public static void beforeEachBaseSSL(TestInfo testInfo, boolean ssl) {
        baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new MySQLProxy("com.mysql.cj.jdbc.Driver",
                mysqlContainer.getJdbcUrl(), null,
                mysqlContainer.getUserId(), mysqlContainer.getPassword());
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
        storage.initialize();
        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var mapper = new JsonMapper();
        errorPlugin = new MySqlNetErrorPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new NetworkErrorPluginSettings().withPercentAction(100));
        latencyPlugin = new MySqlLatencyPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new LatencyPluginSettings().withMinMax(500, 1000).withPercentAction(100));

        var pl = new MySqlRecordPlugin(mapper, storage, new MultiTemplateEngine(), new SimpleParser()).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());

        var pl1 = new MySqlMockPlugin(mapper, storage, new MultiTemplateEngine());
        var global = new GlobalSettings();
        //global.putService("storage", storage);
        var mockPluginSettings = new BasicMockPluginSettings();
        pl1.initialize(global, new JdbcProtocolSettings(), mockPluginSettings);
        var rep = new MySqlReportPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        rep.setActive(true);
        proxy.setPluginHandlers(List.of(pl, pl1, rep, errorPlugin, latencyPlugin));


        pl.setActive(true);
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        baseProtocol.setProxy(proxy);
        var mysqlSettings = (MySqlProtocolSettings) baseProtocol.getSettings();
        mysqlSettings.setUseTls(ssl);
        baseProtocol.initialize();
        protocolServer = new NettyServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void beforeEachBasePrep(TestInfo testInfo) {
        var baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new MySQLProxy("com.mysql.cj.jdbc.Driver",
                mysqlContainer.getJdbcUrl() +
                        "?generateSimpleParameterMetadata=true" +
                        "&useServerPrepStmts=true", null,
                mysqlContainer.getUserId(), mysqlContainer.getPassword());
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
        var global = new GlobalSettings();
        //global.putService("storage", storage);
        storage.initialize();
        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var mapper = new JsonMapper();
        var pl = new MySqlRecordPlugin(mapper, storage, new MultiTemplateEngine(), new SimpleParser()).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());
        proxy.setPluginHandlers(List.of(pl));
        pl.setActive(true);
        var pl1 = new MySqlMockPlugin(mapper, storage, new MultiTemplateEngine());
        var mockPluginSettings = new BasicMockPluginSettings();
        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        proxy.setPluginHandlers(List.of(pl, pl1));
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

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
        mysqlContainer.close();
    }

    protected static Connection getProxyConnection() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.mysql.cj.jdbc.Driver");
        //?sslMode=REQUIRED
        c = DriverManager
                .getConnection(String.format("jdbc:mysql://127.0.0.1:%d?useSSL=false", FAKE_PORT),
                        "root", "test");
        assertNotNull(c);
        return c;
    }

    protected static Connection getProxyConnectionSsl() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.mysql.cj.jdbc.Driver");
        //?sslMode=REQUIRED
        c = DriverManager
                .getConnection(String.format("jdbc:mysql://127.0.0.1:%d?allowCleartextPasswords=true&sslMode=REQUIRED", FAKE_PORT),
                        "root", "test");
        assertNotNull(c);
        return c;
    }

    protected static Connection getProxyConnectionWithPrepStmts() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.mysql.cj.jdbc.Driver");
        c = DriverManager
                .getConnection(String.format("jdbc:mysql://127.0.0.1:%d/?useServerPrepStmts=true&useSSL=false", FAKE_PORT),
                        "root", "test");
        assertNotNull(c);
        return c;
    }

    protected static Connection getRealConnection() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.mysql.cj.jdbc.Driver");
        c = DriverManager
                .getConnection(mysqlContainer.getJdbcUrl(),
                        mysqlContainer.getUserId(), mysqlContainer.getPassword());
        assertNotNull(c);
        return c;
    }

    protected static Connection getRealConnectionPs() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.mysql.cj.jdbc.Driver");
        c = DriverManager
                .getConnection(mysqlContainer.getJdbcUrl() + "?generateSimpleParameterMetadata=true",
                        mysqlContainer.getUserId(), mysqlContainer.getPassword());
        assertNotNull(c);
        return c;
    }

    public List<ReportDataEvent> getEvents() {
        return events.stream().collect(Collectors.toList());
    }
}
