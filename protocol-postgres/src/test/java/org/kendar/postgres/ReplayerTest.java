package org.kendar.postgres;

import org.junit.jupiter.api.Test;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.postgres.jpa.CompanyJpa;
import org.kendar.postgres.plugins.PostgresReplayPlugin;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.storage.FileStorageRepository;
import org.kendar.tcpserver.TcpServer;
import org.kendar.tests.jpa.HibernateSessionFactory;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplayerTest {
    protected static final int FAKE_PORT = 5431;

    @Test
    void simpleJpaTest() throws Exception {
        var baseProtocol = new PostgresProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("org.postgresql.Driver");
        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "replay"));
        storage.initialize();
        var gs = new GlobalSettings();
        var mapper = new JsonMapper();
        //gs.putService("storage", storage);
        var pl = new PostgresReplayPlugin(mapper, storage).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicReplayPluginSettings());
        proxy.setPlugins(List.of(pl));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        try {
            Sleeper.sleep(5000, protocolServer::isRunning);

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
        } finally {
            protocolServer.stop();
        }

    }
}
