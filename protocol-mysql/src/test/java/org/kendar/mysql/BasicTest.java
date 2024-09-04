package org.kendar.mysql;

import org.junit.jupiter.api.TestInfo;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.testcontainer.images.MysqlImage;
import org.kendar.testcontainer.utils.Utils;
import org.kendar.utils.Sleeper;
import org.testcontainers.containers.Network;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 3310;
    protected static MysqlImage mysqlContainer;
    protected static TcpServer protocolServer;

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
        var baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("com.mysql.cj.jdbc.Driver",
                mysqlContainer.getJdbcUrl(), null,
                mysqlContainer.getUserId(), mysqlContainer.getPassword());
        if (testInfo != null) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();
            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                proxy.setStorage(new MySqlFileStorage(Path.of("target", "tests", className, method, dsp)));
            } else {
                proxy.setStorage(new MySqlFileStorage(Path.of("target", "tests", className, method)));
            }
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000,()->protocolServer.isRunning());
    }

    public static void beforeEachBasePrep(TestInfo testInfo) {
        var baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("com.mysql.cj.jdbc.Driver",
                mysqlContainer.getJdbcUrl() +
                        "?generateSimpleParameterMetadata=true" +
                        "&useServerPrepStmts=true", null,
                mysqlContainer.getUserId(), mysqlContainer.getPassword());
        if (testInfo != null) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();
            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                proxy.setStorage(new MySqlFileStorage(Path.of("target", "tests", className, method, dsp)));
            } else {
                proxy.setStorage(new MySqlFileStorage(Path.of("target", "tests", className, method)));
            }
        }
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000,()->protocolServer.isRunning());
    }

    public static void afterEachBase() {

        try {
            protocolServer.stop();
        }catch (Exception ex){

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
