package org.kendar.runner;

import org.kendar.Main;
import org.kendar.server.TcpServer;
import org.kendar.tests.testcontainer.images.PostgresSqlImage;
import org.kendar.tests.testcontainer.utils.Utils;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.testcontainers.containers.Network;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicTest {

    protected static final int FAKE_PORT = 5631;
    protected static PostgresSqlImage postgresContainer;
    protected static TcpServer protocolServer;
    protected static JsonMapper mapper = new JsonMapper();

    protected AtomicBoolean runTheServer = new AtomicBoolean(true);
    protected void startAndHandleUnexpectedErrors(String... args) {
        AtomicReference exception = new AtomicReference(null);
        var serverThread = new Thread(() -> {
            try {
                Main.execute(args, () -> {
                    try {
                        Sleeper.sleep(100);
                        return runTheServer.get();
                    } catch (Exception e) {
                        exception.set(e);
                        return false;
                    }
                });
                exception.set(new Exception("Terminated abruptly"));
            } catch (Exception ex) {
                exception.set(new Exception("Terminated with error", ex));
            }

        });
        serverThread.start();
        while (!Main.isRunning()) {
            if (exception.get() != null) {
                throw new RuntimeException((Throwable) exception.get());
            }
            Sleeper.sleep(100);
        }
    }

    public static void beforeClassBase() {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();
        postgresContainer = new PostgresSqlImage();
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
