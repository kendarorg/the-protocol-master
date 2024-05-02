package org.kendar.postgres;

import org.junit.jupiter.api.*;
import org.kendar.utils.Sleeper;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostgresProtocolTest extends BasicTest {

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void proxyTestTransactions() throws Exception {

        var runned = false;


        Connection c = getProxyConnection();


        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_1 " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();


        c.setAutoCommit(false);
        c.beginRequest();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_1 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        c.setAutoCommit(true);

        c.setAutoCommit(false);
        c.beginRequest();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_1 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (12,'other', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        c.rollback();
        c.setAutoCommit(true);
        stmt = c.createStatement();
        var resultset = stmt.executeQuery("SELECT DENOMINATION FROM COMPANY_1;");
        while (resultset.next()) {
            assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            runned = true;
        }

        resultset.close();
        stmt.close();
        c.close();

        assertTrue(runned);
    }


    @Test
    void simpleProxyTest() throws Exception {

        var runned = false;


        Connection c = getProxyConnection();
        Statement stmt;


        stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_2 " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();

        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_2 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        stmt = c.createStatement();
        var resultset = stmt.executeQuery("SELECT DENOMINATION FROM COMPANY_2;");
        while (resultset.next()) {
            assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            runned = true;
        }


        resultset.close();
        stmt.close();
        c.close();

        assertTrue(runned);


    }


    @Test
    void preparedStatementTest() throws Exception {

        var runned = false;


        try {
            Sleeper.sleep(1000);


            Connection c;
            Statement stmt;
            try {
                c = getProxyConnection();

                stmt = c.createStatement();
                stmt.executeUpdate("CREATE TABLE COMPANY_3 " +
                        "(ID INT PRIMARY KEY NOT NULL," +
                        " DENOMINATION TEXT NOT NULL, " +
                        " AGE INT NOT NULL, " +
                        " ADDRESS CHAR(50), " +
                        " SALARY REAL)");
                stmt.close();

                stmt = c.createStatement();
                stmt.executeUpdate("INSERT INTO COMPANY_3 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                        "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
                stmt.close();
                var pstmt = c.prepareStatement("SELECT DENOMINATION FROM COMPANY_3 WHERE ID=? AND DENOMINATION=?;");
                pstmt.setInt(1, 10);
                pstmt.setString(2, "Test Ltd");
                var resultset = pstmt.executeQuery();
                while (resultset.next()) {
                    assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
                    runned = true;
                }

                resultset.close();
                stmt.close();
                c.close();
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                //System.exit(0);
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
        assertTrue(runned);
    }

    @Test
    void proxyTestSp() throws Exception {

        var runned = false;


        Connection c = getProxyConnection();


        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_SP " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();


        var pstmt = c.prepareStatement("INSERT INTO COMPANY_SP (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,?, 42, 'Ping Road 22', 25000.7);");
        pstmt.setString(1, "Test Ltd");
        pstmt.execute();
        stmt.close();

        pstmt = c.prepareStatement("SELECT DENOMINATION FROM COMPANY_SP WHERE ID=?;");
        pstmt.setInt(1, 10);
        var resultset = pstmt.executeQuery();
        while (resultset.next()) {
            assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            runned = true;
        }

        resultset.close();
        stmt.close();
        c.close();

        assertTrue(runned);
    }


    @Test
    void testCancel() throws Exception {

        Connection c = getProxyConnection();
        Statement stmt;

        //ResultSet resultSet;
        AtomicInteger counter = new AtomicInteger(0);
        stmt = c.createStatement();
        new Thread(() -> {
            try {
                stmt.execute(
                        "SELECT *,pg_sleep(1) as sleep from generate_series(1,10)");
                var resultSet = stmt.getResultSet();
                while (resultSet.next()) {
                    counter.incrementAndGet();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();

        Sleeper.sleep(3000);
        stmt.cancel();
        stmt.close();
        c.close();
        assertEquals(0, counter.get());

    }

    @Test
    void testErrors() throws Exception {

        var runned = false;


        Connection c = getProxyConnection();


        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_1 " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();


        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO WETHEAVER (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();


        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_1 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();
        c.close();

        assertTrue(runned);
    }
}
