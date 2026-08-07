package org.kendar.mssql;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("SqlNoDataSourceInspection")
public class MockTest extends MssqlBasicTest {
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

    private static void createAndFill(Connection c, String tableExt) throws SQLException {
        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_" + tableExt + " " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION VARCHAR(255) NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_" + tableExt + " (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    private void activateMock() {
        baseProtocol.getProxy().getPluginHandlers().stream().filter(a ->
                a.getId().equalsIgnoreCase("mock-plugin")).findFirst().get().setActive(true);
        baseProtocol.getProxy().getPluginHandlers().stream().filter(a ->
                a.getId().equalsIgnoreCase("record-plugin")).findFirst().get().setActive(false);
    }

    @Test
    public void countedMock() throws Exception {
        activateMock();
        var runned = false;
        Connection c = getProxyConnection();
        createAndFill(c, "C");

        runned = verifyRun(c, "FAKED", runned, "C");
        runned = verifyRun(c, "FAKED", runned, "C");
        runned = verifyRun(c, "Test Ltd", runned, "C");
        c.close();

        assertTrue(runned);
    }

    @Test
    public void nthCall() throws Exception {
        activateMock();
        var runned = false;
        Connection c = getProxyConnection();
        createAndFill(c, "N");

        runned = verifyRun(c, "Test Ltd", runned, "N");
        runned = verifyRun(c, "FAKED", runned, "N");
        runned = verifyRun(c, "Test Ltd", runned, "N");
        c.close();

        assertTrue(runned);
    }
}
