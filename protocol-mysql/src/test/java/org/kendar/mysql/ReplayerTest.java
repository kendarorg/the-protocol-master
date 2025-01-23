package org.kendar.mysql;

import org.junit.jupiter.api.Test;
import org.kendar.mysql.jpa.CompanyJpa;
import org.kendar.mysql.plugins.MySqlReplayPlugin;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.storage.FileStorageRepository;
import org.kendar.tcpserver.TcpServer;
import org.kendar.tests.jpa.HibernateSessionFactory;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ReplayerTest {
    protected static final int FAKE_PORT = 5455;

    @Test
    void showWarnings() throws Exception {

        var baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("com.mysql.cj.jdbc.Driver");

        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "showWarnings"));
        storage.initialize();

        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var mapper = new JsonMapper();
        var pl = new MySqlReplayPlugin(mapper, storage).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicReplayPluginSettings());
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, protocolServer::isRunning);
        Connection c;
        Class.forName("com.mysql.cj.jdbc.Driver");
        c = DriverManager
                .getConnection(String.format("jdbc:mysql://127.0.0.1:%d", FAKE_PORT),
                        "root", "test");

        var runned = false;
        var stmt = c.createStatement();
        var resultset = stmt.executeQuery("SHOW WARNINGS");
        while (resultset.next()) {
            runned = true;
        }
        resultset.close();
        stmt.close();
        c.close();

        assertFalse(runned);
        protocolServer.stop();
        Sleeper.sleep(100);
    }

    @Test
    void simpleJpaTest() throws Exception {
        var baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("com.mysql.cj.jdbc.Driver");

        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "replay"));
        storage.initialize();

        var gs = new GlobalSettings();
        //gs.putService("storage", storage);
        var mapper = new JsonMapper();
        var pl = new MySqlReplayPlugin(mapper, storage).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicReplayPluginSettings());
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, protocolServer::isRunning);


        HibernateSessionFactory.initialize("com.mysql.cj.jdbc.Driver",
                String.format("jdbc:mysql://127.0.0.1:%d", FAKE_PORT),
                "test", "test",
                "org.hibernate.dialect.MySQLDialect",
                CompanyJpa.class);
        Sleeper.sleep(1000);

        HibernateSessionFactory.transactional(em -> {
            var lt = new CompanyJpa();
            lt.setDenomination("Test Ltd");
            lt.setAddress("TEST RD");
            lt.setAge(22);
            lt.setSalary(500.22);
            em.persist(lt);
        });
        Sleeper.sleep(1000);
        var atomicBoolean = new AtomicBoolean(false);
        HibernateSessionFactory.query(em -> {
            var resultset = em.createQuery("SELECT denomination FROM CompanyJpa").getResultList();
            for (var rss : resultset) {
                assertEquals("Test Ltd", rss);
                atomicBoolean.set(true);
            }
        });

        assertTrue(atomicBoolean.get());
        protocolServer.stop();
        Sleeper.sleep(100);
    }
}
