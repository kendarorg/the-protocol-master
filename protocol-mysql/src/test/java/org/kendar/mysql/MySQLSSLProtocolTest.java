package org.kendar.mysql;

import org.junit.jupiter.api.*;
import org.kendar.utils.Sleeper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SqlNoDataSourceInspection")
public class MySQLSSLProtocolTest extends MySqlBasicTest {
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
        Sleeper.sleep(1000);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void proxyTestTransactions() throws Exception {

        var runned = false;


        Connection c = getProxyConnectionSsl();


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
        Connection c = getProxyConnectionSsl();
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
    void simpleProxyTestChangingWithWrongUser() throws Exception {

        var runned = false;
        Connection c;
        Class.forName("com.mysql.cj.jdbc.Driver");
        //?sslMode=REQUIRED
        Throwable thrown = null;
        try {
            DriverManager
                    .getConnection(String.format("jdbc:mysql://127.0.0.1:%d?allowCleartextPasswords=true&sslMode=REQUIRED", FAKE_PORT),
                            "rootWrong", "test");
        }catch (Exception ex){
            thrown=ex;
        }
        assertNotNull(thrown);
    }


    @Test
    void testWeirdQuery() throws Exception {

        var runned = false;


        Connection c = getProxyConnectionSsl();


        var stmt = c.createStatement();

        stmt.executeUpdate("CREATE TABLE COMPANY_21 " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO COMPANY_21 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        stmt.close();

        try {
            stmt = c.createStatement();
            stmt.execute(
                    "SELECT current_setting('server_version_num')::int/100 as version;");
            var resultSet = stmt.getResultSet();
            while (resultSet.next()) {
                System.out.println(resultSet.getString(1));
            }
            resultSet.close();
            stmt.close();
        } catch (Exception ex) {

        }
        stmt = c.createStatement();
        var resultset = stmt.executeQuery("SELECT DENOMINATION FROM COMPANY_21;");
        while (resultset.next()) {
            assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            runned = true;
        }
        resultset.close();
        c.close();

    }


}
