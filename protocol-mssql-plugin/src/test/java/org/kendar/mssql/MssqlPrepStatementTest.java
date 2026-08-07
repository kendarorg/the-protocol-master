package org.kendar.mssql;

import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("SqlNoDataSourceInspection")
public class MssqlPrepStatementTest extends MssqlBasicTest {
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
    void preparedStatementTest() throws Exception {

        var runned = false;
        Connection c = getProxyConnection();
        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_PS " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION VARCHAR(255) NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();

        var ps = c.prepareStatement("INSERT INTO COMPANY_PS (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (?,?,?,?,?)");
        ps.setInt(1, 10);
        ps.setString(2, "Test Ltd");
        ps.setInt(3, 42);
        ps.setString(4, "Ping Road 22");
        ps.setDouble(5, 25000.7);
        assertEquals(1, ps.executeUpdate());
        ps.close();

        var query = c.prepareStatement("SELECT DENOMINATION,AGE FROM COMPANY_PS WHERE ID=?");
        query.setInt(1, 10);
        var resultset = query.executeQuery();
        while (resultset.next()) {
            assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            assertEquals(42, resultset.getInt("AGE"));
            runned = true;
        }
        resultset.close();
        query.close();

        //Execute twice to go through the sp_prepare/sp_execute path
        var query2 = c.prepareStatement("SELECT DENOMINATION FROM COMPANY_PS WHERE ID=?");
        for (var i = 0; i < 3; i++) {
            query2.setInt(1, 10);
            var rs = query2.executeQuery();
            while (rs.next()) {
                assertEquals("Test Ltd", rs.getString(1));
            }
            rs.close();
        }
        query2.close();
        c.close();

        assertTrue(runned);
    }
}
