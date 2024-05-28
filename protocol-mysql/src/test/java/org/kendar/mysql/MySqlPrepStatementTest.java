package org.kendar.mysql;

import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MySqlPrepStatementTest extends BasicTest {
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
        beforeEachBasePrep(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void proxyTestSp() throws Exception {

        var runned = false;


        Connection c = getProxyConnectionWithPrepStmts();


        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_1 " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();


        var pstmt = c.prepareStatement("INSERT INTO COMPANY_1 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,?, 42, 'Ping Road 22', 25000.7);");
        pstmt.setString(1, "Test Ltd");
        //pstmt.setString(1,null);
        pstmt.execute();
        stmt.close();

        pstmt = c.prepareStatement("SELECT DENOMINATION FROM COMPANY_1 WHERE ID=?;");
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
}
