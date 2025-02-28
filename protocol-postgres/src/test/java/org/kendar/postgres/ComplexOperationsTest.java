package org.kendar.postgres;

import junit.framework.TestSuite;
import org.junit.jupiter.api.*;
import org.kendar.tests.testcontainer.utils.IOReplicator;
import org.testcontainers.shaded.org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class ComplexOperationsTest extends PostgresBasicTest {

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
    void postgresProxyTestTransactionsNonStandard() throws Exception {

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
    void postgresProcedureTest() throws Exception {


        Connection c = getProxyConnection();
        Statement stmt;


        stmt = c.createStatement();
        stmt.executeUpdate("CREATE OR REPLACE FUNCTION hello_dear(p1 TEXT) RETURNS TEXT "
                + " AS $$ "
                + " BEGIN "
                + " RETURN 'hello ' || p1; "
                + " END; "
                + " $$ "
                + " LANGUAGE plpgsql");
        stmt.close();

        var callableStatement = c.prepareCall("{call hello_dear( ? ) }");
        callableStatement.registerOutParameter(1, Types.VARCHAR);
        // input
        callableStatement.setString(1, "mkyong");

        callableStatement.execute();
        // Get result
        String result = callableStatement.getString(1);
        assertEquals("hello mkyong", result);


        stmt.close();
        c.close();
    }

    @Test
    void postgresProcedureSingleOut() throws Exception {


        Connection c = getProxyConnection();
        Statement stmt;


        stmt = c.createStatement();
        stmt.executeUpdate("CREATE OR REPLACE FUNCTION hello_1(p1 TEXT,p2 out TEXT)"
                + " AS $$ "
                + " BEGIN "
                //+ " p0 := 'fuffa';"
                + " p2 :=  CONCAT('hello ',p1); "
                + " END; "
                + " $$ "
                + " LANGUAGE plpgsql");
        stmt.close();
        CallableStatement callableStatement = c.prepareCall("{? = call hello_1(?) }");
        callableStatement.registerOutParameter(1, Types.VARCHAR);
        callableStatement.setString(2, "pippo");
        callableStatement.execute();
        assertEquals("hello pippo", callableStatement.getString(1));

        stmt.close();
        c.close();
    }

    @Test
    void postgresProcedureMultiOut() throws Exception {


        Connection c = getProxyConnection();
        Statement stmt;


        stmt = c.createStatement();
        stmt.executeUpdate("CREATE OR REPLACE FUNCTION hello(p0 out TEXT,p1 TEXT,p2 out TEXT)"
                + " AS $$ "
                + " BEGIN "
                + " p0 := 'fuffa';"
                + " p2 :=  CONCAT('hello ',p1); "
                + " END; "
                + " $$ "
                + " LANGUAGE plpgsql");
        stmt.close();


        CallableStatement callableStatement = c.prepareCall("{call hello(?,?,?) }");
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
    void postgresProcedureOutInSelect() throws Exception {


        Connection c = getProxyConnection();
        Statement stmt;

        stmt = c.createStatement();
        stmt.executeUpdate("CREATE OR REPLACE FUNCTION hello_3(p0 out TEXT,p1 TEXT,p2 out TEXT)"
                + " AS $$ "
                + " BEGIN "
                + " p0 := 'fuffa';"
                + " p2 :=  CONCAT('hello ',p1); "
                + " END; "
                + " $$ "
                + " LANGUAGE plpgsql");
        stmt.close();

        stmt = c.prepareStatement("select * from hello_3(?)");
        ((PreparedStatement) stmt).setString(1, "pippo");
        // ((PreparedStatement)stmt).setString(2,null);
        //((PreparedStatement)stmt).setString(2,"");
        var res = ((PreparedStatement) stmt).executeQuery();
        while (res.next()) {
            System.out.println(res.getString(1));
            System.out.println(res.getString(2));
        }
        //}

        stmt.close();
        c.close();
    }

    @Test
    void testSimpleQueryProtocol() throws IOException {
        if (!SystemUtils.IS_OS_WINDOWS) {
            TestSuite.warning("Not on windows, cannot test the simple postgres query protocol");
            return;
        }

        var psqlWinExecutable = getRootPath("client\\psql.exe");
        assertTrue(Files.exists(psqlWinExecutable));

        ProcessBuilder builder = new ProcessBuilder();
        String connUrl = "postgresql://" +
                //postgresContainer.getUserId()+":"+
                "root:" +
                postgresContainer.getPassword() + "@localhost:" +
                FAKE_PORT
                + "/" + postgresContainer.getDbName();
        builder.command(psqlWinExecutable.toString(), connUrl);
        Process process = builder.start();
        var ioReplicator = new IOReplicator(process);
        ioReplicator.showData();
        ioReplicator.write("CREATE TABLE COMPANY_2 " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION TEXT NOT NULL, " +
                " AGE INT NOT NULL, " +
                " ADDRESS CHAR(50), " +
                " SALARY REAL);");
        ioReplicator.write("INSERT INTO COMPANY_2 (ID,DENOMINATION, AGE, ADDRESS, SALARY) " +
                "VALUES (10,'Test Ltd', 42, 'Ping Road 22', 25000.7);");
        var result = ioReplicator.write("SELECT DENOMINATION FROM COMPANY_2;");
        assertNotNull(result);
        assertTrue(result.contains("Test Ltd"));
        assertTrue(result.toLowerCase().contains("denomination"));
        assertTrue(result.toLowerCase().contains("(1 row)"));
        process.destroyForcibly();

    }

}
