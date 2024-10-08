package org.kendar.postgres;

import org.junit.jupiter.api.TestInfo;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.storage.JdbcFileStorage;
import org.kendar.testcontainer.images.PostgreslImage;
import org.kendar.testcontainer.utils.Utils;
import org.kendar.utils.Sleeper;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 5431;
    protected static PostgreslImage postgresContainer;
    protected static TcpServer protocolServer;

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        postgresContainer = new PostgreslImage();
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

        var baseProtocol = new PostgresProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("org.postgresql.Driver",
                postgresContainer.getJdbcUrl(), null,
                postgresContainer.getUserId(), postgresContainer.getPassword());


        if (testInfo != null && testInfo.getTestClass().isPresent() &&
                testInfo.getTestMethod().isPresent()) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();
            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                proxy.setStorage(new JdbcFileStorage(Path.of("target", "tests", className, method, dsp)));
            } else {
                proxy.setStorage(new JdbcFileStorage(Path.of("target", "tests", className, method)));
            }
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
    }

    public static void afterEachBase() {
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
}
