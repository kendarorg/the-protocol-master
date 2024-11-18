package org.kendar.mysql;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MockTest extends BasicTest {
    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
    }

    private static boolean verifyRun(Connection c, String expectedResult, boolean runned, String tableExt) throws SQLException {
        Statement stmt;
        stmt = c.createStatement();
        var resultset = stmt.executeQuery("SELECT DENOMINATION FROM COMPANY_" + tableExt + ";");
        while (resultset.next()) {
            assertEquals(expectedResult, resultset.getString("DENOMINATION"));
            runned = true;
        }
        resultset.close();
        stmt.close();
        return runned;
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
    public void countedMock() throws Exception {
        baseProtocol.getProxy().getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("mock-plugin")).findFirst().get().setActive(true);
        baseProtocol.getProxy().getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("record-plugin")).findFirst().get().setActive(false);
        var runned = false;
        Connection c = getProxyConnection();
        Statement stmt;
        stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_C " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_C (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        runned = verifyRun(c, "FAKED", runned, "C");
        runned = verifyRun(c, "FAKED", runned, "C");
        runned = verifyRun(c, "Test Ltd", runned, "C");
        c.close();

        assertTrue(runned);
    }

    @Test
    public void nthCall() throws Exception {
        baseProtocol.getProxy().getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("mock-plugin")).findFirst().get().setActive(true);
        baseProtocol.getProxy().getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("record-plugin")).findFirst().get().setActive(false);
        var runned = false;
        Connection c = getProxyConnection();
        Statement stmt;
        stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_N " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_N (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        runned = verifyRun(c, "Test Ltd", runned, "N");
        runned = verifyRun(c, "FAKED", runned, "N");
        runned = verifyRun(c, "Test Ltd", runned, "N");
        c.close();

        assertTrue(runned);
    }


    @Test
    public void both() throws Exception {
        baseProtocol.getProxy().getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("mock-plugin")).findFirst().get().setActive(true);
        baseProtocol.getProxy().getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("record-plugin")).findFirst().get().setActive(false);
        var runned = false;
        Connection c = getProxyConnection();
        Statement stmt;
        stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_B " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_B (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        runned = verifyRun(c, "Test Ltd", runned, "B");
        runned = verifyRun(c, "FAKED", runned, "B");
        runned = verifyRun(c, "FAKED", runned, "B");
        runned = verifyRun(c, "FAKED", runned, "B");
        runned = verifyRun(c, "Test Ltd", runned, "B");
        c.close();

        assertTrue(runned);
    }

    @Test
    void replacingMockQuery() throws Exception {
        baseProtocol.getProxy().getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("mock-plugin")).findFirst().get().setActive(true);
        baseProtocol.getProxy().getPlugins().stream().filter(a ->
                a.getId().equalsIgnoreCase("record-plugin")).findFirst().get().setActive(false);
        var runned = false;
        Connection c = getProxyConnection();
        Statement stmt;
        stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_R " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_R (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        var pstmt = c.prepareStatement("SELECT ADDRESS,AGE FROM COMPANY_R WHERE DENOMINATION='FAKEDENOMINATION' AND AGE=22;");
        var resultset = pstmt.executeQuery();
        while (resultset.next()) {
            assertEquals("FAKEDENOMINATION", resultset.getString("ADDRESS"));
            assertEquals(22, resultset.getInt("AGE"));
            runned = true;
        }
        resultset.close();
        stmt.close();
        c.close();

        assertTrue(runned);
    }
}
