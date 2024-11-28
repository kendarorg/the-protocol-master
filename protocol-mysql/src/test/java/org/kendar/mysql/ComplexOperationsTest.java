package org.kendar.mysql;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComplexOperationsTest extends BasicTest {

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
    void mysqlProxyTestTransactionsNonStandard() throws Exception {

        var runned = false;


        Connection c = getProxyConnection();
        Statement stmt;


        c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);


        stmt = c.createStatement();
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

    public Path getRootPath(String targetDir) throws IOException {
        if (!Path.of(targetDir).isAbsolute()) {
            Path currentRelativePath = Paths.get("").toAbsolutePath();
            targetDir = Path.of(currentRelativePath.toString(), targetDir).toString();
        }
        return Path.of(targetDir);
    }

    @Test
    void mysqlFunctionTest() throws Exception {


        Connection c = getProxyConnection();
        Statement stmt;


        stmt = c.createStatement();
        stmt.executeUpdate("CREATE FUNCTION hello_dear( p1 text) RETURNS text DETERMINISTIC " +
                " RETURN CONCAT('hello ', p1);");
        stmt.close();

        var callableStatement = c.prepareCall("{?=call test.hello_dear( ? ) }");
        callableStatement.registerOutParameter(1, Types.VARCHAR);
        // input
        callableStatement.setString(2, "mkyong");

        callableStatement.execute();
        // Get result
        String result = callableStatement.getString(1);
        assertEquals("hello mkyong", result);


        stmt.close();
        c.close();
    }

    @Test
    void mysqlProcedureSingleOut() throws Exception {


        Connection c = getProxyConnection();
        Statement stmt;


        stmt = c.createStatement();
        stmt.executeUpdate("CREATE PROCEDURE hello_1(p1 TEXT,out p2 TEXT)"
                + " BEGIN "
                + " SET p2 = CONCAT('hello ',p1); "
                + " END");
        stmt.close();
        CallableStatement callableStatement = c.prepareCall("{call test.hello_1(?,?)}");
        callableStatement.setString(1, "pippo");

        callableStatement.registerOutParameter(2, Types.VARCHAR);
        callableStatement.execute();
        assertEquals("hello pippo", callableStatement.getString(2));

        stmt.close();
        c.close();
    }

    @Test
    void mysqlProcedureMultiOut() throws Exception {


        Connection c = getProxyConnection();
        Statement stmt;


        stmt = c.createStatement();
        stmt.executeUpdate("CREATE PROCEDURE hello(out p0  TEXT,p1 TEXT,out p2  TEXT)"
                + " BEGIN "
                + " set p0 = 'fuffa';"
                + " set p2 =  CONCAT('hello ',p1); "
                + " END; "

        );
        stmt.close();


        CallableStatement callableStatement = c.prepareCall("{call test.hello(?,?,?) }");
        callableStatement.registerOutParameter(1, Types.VARCHAR);
        callableStatement.setString(2, "pippo");
        callableStatement.registerOutParameter(3, Types.VARCHAR);
        callableStatement.execute();
        assertEquals("fuffa", callableStatement.getString(1));
        assertEquals("hello pippo", callableStatement.getString(3));

        stmt.close();
        c.close();
    }

    @Test
    void testVersion() throws SQLException, ClassNotFoundException {
        Connection c = getRealConnection();
        var st = c.createStatement();
        st.execute("SELECT VERSION()");
        var rs = st.getResultSet();
        while (rs.next()) {
            System.out.println(rs.getString(1));
        }
    }
}
