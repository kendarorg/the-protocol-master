package org.kendar.mysql;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MySQLProtocolTest extends BasicTest {
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
}
