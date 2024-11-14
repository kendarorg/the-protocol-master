package org.kendar.runner;

import org.junit.jupiter.api.*;
import org.kendar.Main;
import org.kendar.jpa.HibernateSessionFactory;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiRecordReplay extends BasicTest {

    private AtomicBoolean runTheServer = new AtomicBoolean(true);

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        runTheServer.set(true);
    }

    @AfterEach
    public void afterEach() {
        runTheServer.set(false);
        Main.stop();
        Sleeper.sleep(100);
    }


    @Test
    void testRecordingReplaying() throws Exception {
        System.out.println("RECORDING ==============================================");
        var timestampForThisRun = "" + new Date().getTime();
        //RECORDING
        var args = new String[]{

                "-datadir", Path.of("target", "tests", timestampForThisRun).toString(),
                "-loglevel", "DEBUG",
                "-protocol", "postgres",
                "-port", "" + FAKE_PORT,
                "-login", postgresContainer.getUserId(),
                "-password", postgresContainer.getPassword(),
                "-connection", postgresContainer.getJdbcUrl(),
                "-record"
        };


        startAndHandleUnexpectedErrors(args);


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
        Main.stop();
        assertTrue(verifyTestRun.get());

        System.out.println("REPLAYING ==============================================");

        //REPLAYING
        runTheServer.set(true);
        verifyTestRun.set(false);

        var replayArgs = new String[]{
                "-datadir", Path.of("target", "tests", timestampForThisRun).toString(),
                "-loglevel", "DEBUG",
                "-protocol", "postgres",
                "-port", "" + FAKE_PORT,
                "-login", postgresContainer.getUserId(),
                "-password", postgresContainer.getPassword(),
                "-connection", postgresContainer.getJdbcUrl(),
                "-replay"
        };


        startAndHandleUnexpectedErrors(replayArgs);

        System.out.println("START SIMULATION ==============================================");
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


        System.out.println("COMPLETED SIMULATION ==============================================");
        runTheServer.set(false);
        Main.stop();
        assertTrue(verifyTestRun.get());
    }

    private void startAndHandleUnexpectedErrors(String[] args) {
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
}
