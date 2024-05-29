package org.kendar.runner;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.jpa.HibernateSessionFactory;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest extends BasicTest {

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
        Main.stop();
        Sleeper.sleep(100);
    }

    @Test
    void testErrorInsert() throws Exception {
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
        while (!Main.isRunning()) {
            Sleeper.sleep(100);
        }


        Connection c = getProxyConnection();


        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_1 " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();


        try {
            stmt = c.createStatement();
            stmt.executeUpdate("INSERT INTO WETHEAVER (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                    "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
            stmt.close();
        } catch (Exception ex) {
            Sleeper.sleep(100);
        }

        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_1 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();
        c.close();

        runTheServer.set(false);
        Main.stop();
    }

    @Test
    void testRecordingReplaying() throws Exception {
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
        while (!Main.isRunning()) {
            Sleeper.sleep(100);
        }


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
        while (!Main.isRunning()) {
            Sleeper.sleep(100);
        }


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
        Main.stop();
        assertTrue(verifyTestRun.get());
    }

    @Test
    void testTimeout() throws Exception {
        var timestampForThisRun = "" + new Date().getTime();
        var args = new String[]{
                "-p", "postgres",
                "-l", "" + FAKE_PORT,
                "-xl", postgresContainer.getUserId(),
                "-xw", postgresContainer.getPassword(),
                "-xc", postgresContainer.getJdbcUrl(),
                "-xd", Path.of("target", "tests", timestampForThisRun).toString(),
                "-v", "DEBUG",
                "-t", "4"
        };

        var serverThread = new Thread(() -> {
            Main.execute(args, () -> {
                Sleeper.sleep(100);
                return runTheServer.get();
            });
        });
        serverThread.start();
        while (!Main.isRunning()) {
            Sleeper.sleep(100);
        }


        Connection c = getProxyConnection();


        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_GG " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();

        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_GG (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        try {
            stmt = c.createStatement();
            var resultset = stmt.executeQuery("SELECT DENOMINATION FROM COMPANY_GG;");
            while (resultset.next()) {

            }
        } catch (Exception ex) {
            Sleeper.sleep(100);
        }

        Sleeper.sleep(15 * 1000);
        stmt = c.createStatement();
        var resultset = stmt.executeQuery("SELECT DENOMINATION FROM COMPANY_GG;");
        while (resultset.next()) {

        }

        c.close();

        runTheServer.set(false);
        Main.stop();
    }

    @Test
    void testErrorSelect() throws Exception {
        var timestampForThisRun = "" + new Date().getTime();
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
        while (!Main.isRunning()) {
            Sleeper.sleep(100);
        }


        Connection c = getProxyConnection();


        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_N " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();


        try {
            stmt = c.createStatement();
            var resultset = stmt.executeQuery("SELECT DENOMINATION FROM COMPANY_XXXXXX;");
            while (resultset.next()) {
                assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            }
        } catch (Exception ex) {
            Sleeper.sleep(100);
        }

        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_N (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();
        c.close();

        runTheServer.set(false);
        Main.stop();
    }
}
