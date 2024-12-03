package org.kendar.mysql;

import org.junit.jupiter.api.TestInfo;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.mysql.plugins.MySqlMockPlugin;
import org.kendar.mysql.plugins.MySqlRecordPlugin;
import org.kendar.mysql.plugins.MySqlReportPlugin;
import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.server.TcpServer;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tests.testcontainer.images.MysqlImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.utils.Sleeper;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 3310;
    protected static MysqlImage mysqlContainer;
    protected static TcpServer protocolServer;
    protected static MySQLProtocol baseProtocol;

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
        baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("com.mysql.cj.jdbc.Driver",
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
            } else {
                storage = new FileStorageRepository(Path.of("target", "tests", className, method));
            }
        }
        storage.initialize();
        var gs = new GlobalSettings();
        gs.putService("storage", storage);
        var pl = new MySqlRecordPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());

        var pl1 = new MySqlMockPlugin();
        var global = new GlobalSettings();
        global.putService("storage", storage);
        var mockPluginSettings = new BasicMockPluginSettings();
        mockPluginSettings.setDataDir(Path.of("src", "test", "resources", "mock").toAbsolutePath().toString());
        pl1.initialize(global, new JdbcProtocolSettings(), mockPluginSettings);
        var rep = new MySqlReportPlugin().initialize(gs,new ByteProtocolSettingsWithLogin(),new PluginSettings());
        rep.setActive(true);
        proxy.setPlugins(List.of(pl, pl1,rep));


        pl.setActive(true);
        EventsQueue.register("recorder",(r)->{
            events.add(r);
        }, ReportDataEvent.class);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    private static ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();
    public List<ReportDataEvent> getEvents(){
        return events.stream().collect(Collectors.toList());
    }
    public static void beforeEachBasePrep(TestInfo testInfo) {
        var baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("com.mysql.cj.jdbc.Driver",
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
            } else {
                storage = new FileStorageRepository(Path.of("target", "tests", className, method));
            }
        }
        var global = new GlobalSettings();
        global.putService("storage", storage);
        storage.initialize();
        var gs = new GlobalSettings();
        gs.putService("storage", storage);
        var pl = new MySqlRecordPlugin().initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);
        var pl1 = new MySqlMockPlugin();
        var mockPluginSettings = new BasicMockPluginSettings();
        mockPluginSettings.setDataDir(Path.of("src", "test", "resources", "mock").toAbsolutePath().toString());
        pl1.initialize(global, new JdbcProtocolSettings(), mockPluginSettings);
        EventsQueue.register("recorder",(r)->{
            events.add(r);
        }, ReportDataEvent.class);
        proxy.setPlugins(List.of(pl, pl1));
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
        c = DriverManager
                .getConnection(String.format("jdbc:mysql://127.0.0.1:%d", FAKE_PORT),
                        "root", "test");
        assertNotNull(c);
        return c;
    }

    protected static Connection getProxyConnectionWithPrepStmts() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("com.mysql.cj.jdbc.Driver");
        c = DriverManager
                .getConnection(String.format("jdbc:mysql://127.0.0.1:%d/?useServerPrepStmts=true", FAKE_PORT),
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
}
