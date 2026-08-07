package org.kendar.mssql;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("SqlNoDataSourceInspection")
public class MssqlSSLProtocolTest extends MssqlBasicTest {
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
        beforeEachBaseSSL(testInfo, true);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void simpleProxyTestSsl() throws Exception {

        var runned = false;
        Connection c = getProxyConnectionSsl();
        Statement stmt;
        stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_SSL " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION VARCHAR(255) NOT NULL)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_SSL (ID,DENOMINATION) " +
                "VALUES (10,'Test Ltd');");
        stmt.close();

        stmt = c.createStatement();
        var resultset = stmt.executeQuery("SELECT DENOMINATION FROM COMPANY_SSL;");
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
