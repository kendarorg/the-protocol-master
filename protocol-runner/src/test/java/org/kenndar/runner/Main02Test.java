package org.kenndar.runner;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main02Test extends BasicTest {

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
    void testErrorInsert() throws Exception {
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
        Sleeper.sleep(1000);
    }
}
