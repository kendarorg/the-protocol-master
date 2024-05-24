package org.kenndar.runner;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.jpa.HibernateSessionFactory;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Main01Test extends BasicTest {

    private AtomicBoolean runTheServer = new AtomicBoolean(true);

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
    }

    @AfterEach
    public void afterEach() {
        runTheServer.set(false);
        Sleeper.sleep(100);
    }

    @Test
    void testMain() throws Exception {
        System.out.println("RECORDING ==============================================");
        var timestampForThisRun = "" + new Date().getTime();
        //RECORDING
        var args = new String[]{
                "-p", "postgres",
                "-l", "" + FAKE_PORT,
                "-xl", postgresContainer.getUserId(),
                "-xw", postgresContainer.getPassword(),
                "-xc", postgresContainer.getJdbcUrl(),
                "-xd", Path.of("target", "tests", timestampForThisRun).toString(),
                "-v", "DEBUG"
        };

        var serverThread = new Thread(() -> {
            Main.execute(args, () -> {
                Sleeper.sleep(100);
                return runTheServer.get();
            });
        });
        serverThread.start();
        Sleeper.sleep(3000);


        HibernateSessionFactory.initialize("org.postgresql.Driver",
                //postgresContainer.getJdbcUrl(),
                String.format("jdbc:postgresql://127.0.0.1:%d/test?ssl=false", FAKE_PORT),
                postgresContainer.getUserId(), postgresContainer.getPassword(),
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
        var verifyTestRun = new AtomicBoolean(false);
        HibernateSessionFactory.query(em -> {
            var resultset = em.createQuery("SELECT denomination FROM CompanyJpa").getResultList();
            for (var rss : resultset) {
                assertEquals("Test Ltd", rss);
                verifyTestRun.set(true);
            }
        });

        runTheServer.set(false);
        Sleeper.sleep(3000);
        serverThread.stop();
        assertTrue(verifyTestRun.get());

        System.out.println("REPLAYING ==============================================");

        //REPLAYING
        runTheServer.set(true);
        verifyTestRun.set(false);

        Sleeper.sleep(2000);
        var replayArgs = new String[]{
                "-p", "postgres",
                "-l", "" + FAKE_PORT,
                "-xl", postgresContainer.getUserId(),
                "-xw", postgresContainer.getPassword(),
                "-xc", postgresContainer.getJdbcUrl(),
                "-xd", Path.of("target", "tests", timestampForThisRun).toString(),
                "-pl",
                "-v", "DEBUG"
        };

        serverThread = new Thread(() -> {
            Main.execute(replayArgs, () -> {
                Sleeper.sleep(100);
                return runTheServer.get();
            });
        });
        serverThread.start();
        Sleeper.sleep(5000);


        HibernateSessionFactory.initialize("org.postgresql.Driver",
                //postgresContainer.getJdbcUrl(),
                String.format("jdbc:postgresql://127.0.0.1:%d/test?ssl=false", FAKE_PORT),
                postgresContainer.getUserId(), postgresContainer.getPassword(),
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
        verifyTestRun.set(false);
        HibernateSessionFactory.query(em -> {
            var resultset = em.createQuery("SELECT denomination FROM CompanyJpa").getResultList();
            for (var rss : resultset) {
                assertEquals("Test Ltd", rss);
                verifyTestRun.set(true);
            }
        });

        runTheServer.set(false);
        assertTrue(verifyTestRun.get());
    }


}
