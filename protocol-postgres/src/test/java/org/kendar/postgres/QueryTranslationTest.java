package org.kendar.postgres;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kendar.server.TcpServer;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.utils.QueryReplacerItem;
import org.kendar.utils.Sleeper;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryTranslationTest extends BasicTest {

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
    }

    public static Stream<Arguments> createInsertSelect() {
        return Stream.of(
                Arguments.of(false, "SELECT \r\n 1 AS TEST", "SELECT \r\n 2 AS TEST", "SELECT \n 1 AS TEST", "2"),
                Arguments.of(true, "SELECT \r\n ([0-9]+) AS TEST", "SELECT \r\n $1+2 AS TEST", "SELECT \n 1 AS TEST", "3"),
                Arguments.of(true, "SELECT \r\n ([0-9]+) AS TEST", "SELECT \r\n 2 AS TEST", "SELECT \n 1 AS TEST", "2")
        );
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {

    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @ParameterizedTest
    @MethodSource("createInsertSelect")
    void testSimpleReplace(boolean regex, String find, String replace, String execute, String result) throws Exception {

        var baseProtocol = new PostgresProtocol(FAKE_PORT);
        var proxy = new JdbcProxy("org.postgresql.Driver",
                postgresContainer.getJdbcUrl(), null,
                postgresContainer.getUserId(), postgresContainer.getPassword());

        var replaceList = new ArrayList<QueryReplacerItem>();
        var replaceItem = new QueryReplacerItem();
        replaceItem.setToFind(find);
        replaceItem.setRegex(regex);
        replaceItem.setToReplace(replace);
        replaceList.add(replaceItem);
        proxy.setQueryReplacement(replaceList);

        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        protocolServer = new TcpServer(baseProtocol);

        protocolServer.start();
        while (!protocolServer.isRunning()) {
            Sleeper.sleep(100);
        }


        Connection c = getProxyConnection();

        var runned = false;
        var stmt = c.createStatement();
        stmt.execute(execute);
        var resultSet = stmt.getResultSet();
        while (resultSet.next()) {
            runned = true;
            assertEquals(result, resultSet.getString(1));
        }
        assertTrue(runned);
        resultSet.close();
        stmt.close();
        c.close();

    }
}
