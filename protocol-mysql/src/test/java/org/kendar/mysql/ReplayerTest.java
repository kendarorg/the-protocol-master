package org.kendar.mysql;

import org.junit.jupiter.api.Test;
import org.kendar.jpa.HibernateSessionFactory;
import org.kendar.mysql.jpa.CompanyJpa;
import org.kendar.mysql.plugins.MySqlReplayPlugin;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.storage.FileStorageRepository;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplayerTest {
    protected static final int FAKE_PORT = 5455;

    @Test
    void simpleJpaTest() throws Exception {
        var baseProtocol = new MySQLProtocol(FAKE_PORT);
        var proxy = new JdbcProxy();

        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "replay"));
        storage.initialize();
        proxy.setPlugins(List.of(new MySqlReplayPlugin().withStorage(storage).asActive()));
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, protocolServer::isRunning);


        HibernateSessionFactory.initialize("com.mysql.cj.jdbc.Driver",
                //postgresContainer.getJdbcUrl(),
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
    }
}
