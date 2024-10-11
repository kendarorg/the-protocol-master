package org.kendar.runner;

import org.kendar.server.TcpServer;
import org.kendar.testcontainer.images.PostgreslImage;
import org.kendar.testcontainer.utils.Utils;
import org.testcontainers.containers.Network;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 5631;
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


    }


    public static void afterClassBase() throws Exception {
        postgresContainer.close();
    }

    protected static Connection getProxyConnection() throws ClassNotFoundException, SQLException {
        Connection c;
        Class.forName("org.postgresql.Driver");
        c = DriverManager
                .getConnection(String.format("jdbc:postgresql://127.0.0.1:%d/test?ssl=false", FAKE_PORT),
                        "root", "test");
        assertNotNull(c);
        return c;
    }
}
