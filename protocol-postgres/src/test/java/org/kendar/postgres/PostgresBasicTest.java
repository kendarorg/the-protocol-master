package org.kendar.postgres;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.TestInfo;
import org.kendar.events.EventsQueue;
import org.kendar.events.ReportDataEvent;
import org.kendar.plugins.base.ProtocolPluginDescriptor;
import org.kendar.plugins.settings.BasicMockPluginSettings;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.postgres.plugins.*;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.settings.PluginSettings;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.NullStorageRepository;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.tcpserver.TcpServer;
import org.kendar.tests.testcontainer.images.PostgresSqlImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
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

public class PostgresBasicTest {

    protected static final int FAKE_PORT = 5431;
    protected static PostgresSqlImage postgresContainer;
    protected static TcpServer protocolServer;
    protected static PostgresProtocol baseProtocol;
    protected static StorageRepository storage;
    private static ConcurrentLinkedQueue<ReportDataEvent> events = new ConcurrentLinkedQueue<>();
    protected static ProtocolPluginDescriptor errorPlugin;
    private static ProtocolPluginDescriptor latencyPlugin;

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        postgresContainer = new PostgresSqlImage();
        postgresContainer
                .withNetwork(network)
                .start();
        Sleeper.sleep(60000, () -> {
            try {
                DriverManager.getConnection(
                        postgresContainer.getJdbcUrl(),
                        postgresContainer.getUserId(), postgresContainer.getPassword());
                return true;
            } catch (SQLException e) {
                return false;
            }
        });
    }

    public static void beforeEachBase(TestInfo testInfo) {

        baseProtocol = new PostgresProtocol(FAKE_PORT);
        var proxy = new PostgresProxy("org.postgresql.Driver",
                postgresContainer.getJdbcUrl(), null,
                postgresContainer.getUserId(), postgresContainer.getPassword());

        storage = new NullStorageRepository();

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
        //gs.putService("storage", storage);
        var pl = new PostgresRecordPlugin(mapper, storage,new MultiTemplateEngine()).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicRecordPluginSettings());
        var pl1 = new PostgresMockPlugin(mapper, storage);
        errorPlugin= new PostgresNetErrorPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(),new NetworkErrorPluginSettings().withPercentAction(100));
        latencyPlugin= new PostgresLatencyPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(),new LatencyPluginSettings().withMinMax(500,1000).withPercentAction(100));

        var mockPluginSettings = new BasicMockPluginSettings();
        var global = new GlobalSettings();
        //global.putService("storage", storage);
        pl1.initialize(global, new JdbcProtocolSettings(), mockPluginSettings);
        var rep = new PostgresReportPlugin(mapper).initialize(gs, new ByteProtocolSettingsWithLogin(), new PluginSettings());
        rep.setActive(true);
        proxy.setPluginHandlers(List.of(pl, pl1, rep,errorPlugin,latencyPlugin));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();

        EventsQueue.register("recorder", (r) -> {
            events.add(r);
        }, ReportDataEvent.class);
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {

        EventsQueue.unregister("recorder", ReportDataEvent.class);
        events.clear();
        protocolServer.stop();
    }

    public static void afterClassBase() throws Exception {
        postgresContainer.close();
    }

    protected static Connection getProxyConnection() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("org.postgresql.Driver");
        c = DriverManager
                .getConnection(String.format("jdbc:postgresql://127.0.0.1:%d/test", FAKE_PORT),//?ssl=false
                        "root", "test");
        assertNotNull(c);
        return c;
    }

    protected static Connection getRealConnection() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("org.postgresql.Driver");
        c = DriverManager
                .getConnection(postgresContainer.getJdbcUrl(),
                        postgresContainer.getUserId(), postgresContainer.getPassword());
        assertNotNull(c);
        return c;
    }

    public List<ReportDataEvent> getEvents() {
        return events.stream().collect(Collectors.toList());
    }
}
