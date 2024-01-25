package org.kendar.mysql;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DataTypesTest extends BasicTest {
    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
    }

    private static Object getValue(String functionName, ResultSet result, PreparedStatement pstmt) throws IllegalAccessException, InvocationTargetException {
        var mt = Arrays.stream(result.getClass().getMethods()).
                filter(m -> m.getName().equalsIgnoreCase("get" + functionName)
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == int.class).
                findFirst().get();
        mt.setAccessible(true);
        var ob = mt.invoke(result, 1);
        return ob;
    }

    private static void setValue(String functionName, Object expected, PreparedStatement pstmt) throws IllegalAccessException, InvocationTargetException, InvocationTargetException {
        var mt = Arrays.stream(pstmt.getClass().getMethods()).
                filter(m -> m.getName().equalsIgnoreCase("set" + functionName)
                        && m.getParameterCount() == 2
                        && m.getParameterTypes()[0] == int.class).
                findFirst().get();
        mt.setAccessible(true);
        mt.invoke(pstmt, 1, expected);
    }

    public static Stream<Arguments> createInsertSelect() {
        return Stream.of(
                //Arguments.of("bigserial",),
                //Arguments.of("bit","Boolean",false),
                //Arguments.of("bit","Boolean",true),
                //Arguments.of("bit varying",),
                //Arguments.of("box",),
                //TODO Arguments.of("bytea","Array",new byte[]{1,2,3}),
                //Arguments.of("cidr",),
                //Arguments.of("circle",),
                //Arguments.of("inet",),
                //Arguments.of("interval",),
                //TODO Arguments.of("json",),
                //Arguments.of("jsonb",),
                //Arguments.of("line",),
                //Arguments.of("lseg",),
                //Arguments.of("macaddr",),
                //TODOArguments.of("money",),
                //Arguments.of("path",),
                //Arguments.of("point",),
                //Arguments.of("polygon",),
                //Arguments.of("smallserial",),
                //Arguments.of("serial",),
                //TODO Arguments.of("time with time zone",),

                //TODO Arguments.of("timestamp with time zone",),
                //Arguments.of("tsquery",),
                //Arguments.of("tsvector",),
                //Arguments.of("txid_snapshot",),
                //Arguments.of("uuid",),
                //TODO Arguments.of("xml",)
                Arguments.of("blob", "Bytes", new byte[]{1,2,3,4}),
                Arguments.of("timestamp", "Timestamp",
                        Timestamp.valueOf("2022-03-05 12:38:18")),
                Arguments.of("time|time", "Time",
                        new Time(4, 5, 6)),
                Arguments.of("date_ext|date", "Date",
                        new Date(Date.from(
                                        Instant.parse("2022-04-01T22:12:15Z")).
                                toInstant().toEpochMilli())),
                Arguments.of("date", "Date", Date.valueOf("2022-04-01")),
                Arguments.of("bigint", "Long", 100L),
                Arguments.of("boolean", "Boolean", false),
                Arguments.of("double_precision|double precision", "Double", 22.77),
                Arguments.of("integer", "Int", 110),
                Arguments.of("real", "Float", (float) 20.22),
                Arguments.of("smallint", "Short", (short) 55),
                Arguments.of("text", "String", "THAT'S RIGHT"),
                Arguments.of("character", "String", "A"),
                Arguments.of("character_4|character(4)", "String", "   B"),
                Arguments.of("char", "String", "B"),
                Arguments.of("numeric|numeric(6,2)", "BigDecimal", BigDecimal.valueOf(-5898.22))
        );
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {

        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @ParameterizedTest
    @MethodSource("createInsertSelect")
    void createInsertSelectTest(String fullType,
                                String functionName,
                                Object expected) throws SQLException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        Connection c = getProxyConnection();

        var fullTypeAr = fullType.split("\\|");
        var nameType = fullTypeAr[0];
        var type = nameType;
        if (fullTypeAr.length > 1) {
            type = fullTypeAr[1];
        }
        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE DTT_" + nameType +
                "(VALUE " + type + " NOT NULL)");
        stmt.close();

        var pstmt = c.prepareStatement("INSERT INTO DTT_" + nameType + " (VALUE) " +
                "VALUES (?);");
        setValue(functionName, expected, pstmt);
        pstmt.execute();
        stmt.close();

        var estmt = c.createStatement();
        var result = estmt.executeQuery("SELECT * FROM DTT_" + nameType);
        assertTrue(result.next());
        var ob = getValue(functionName, result, pstmt);
        if (ob.getClass().isArray() && !(ob instanceof String)) {
            var obl = Arrays.asList(ob);
            var exl = Arrays.asList(expected);
            assertArrayEquals(exl.toArray(), obl.toArray());
        } else {
            assertEquals(expected.toString(), ob.toString());
        }
        stmt.close();


        stmt = c.createStatement();
        stmt.executeUpdate("DROP TABLE DTT_" + nameType);
        stmt.close();

    }
}
