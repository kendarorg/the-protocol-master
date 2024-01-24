package org.kendar.postgres;

import org.junit.jupiter.api.Test;
import org.kendar.jpa.HibernateSessionFactory;
import org.kendar.postgres.jpa.CompanyJpa;
import org.kendar.protocol.Sleeper;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcReplayProxy;
import org.kendar.sql.jdbc.storage.JdbcFileStorage;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplayerTest {
    protected static final int FAKE_PORT = 5431;
    @Test
    void test() throws Exception {
        var baseProtocol = new PostgresProtocol(FAKE_PORT);
        var proxy = new JdbcReplayProxy(new JdbcFileStorage(Path.of("src",
                "test","resources","replay")));
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(1000);


        HibernateSessionFactory.initialize("org.postgresql.Driver",
                //postgresContainer.getJdbcUrl(),
                String.format("jdbc:postgresql://127.0.0.1:%d/test?ssl=false", FAKE_PORT),
               "uid", "pwd",
                "org.hibernate.dialect.PostgreSQLDialect",
                CompanyJpa.class);

        HibernateSessionFactory.transactional(em -> {
            var lt = new CompanyJpa();
            lt.setDenomination("Test Ltd");
            lt.setAddress("TEST RD");
            lt.setAge(22);
            lt.setSalary(500.22);
            em.persist(lt);
        });
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
