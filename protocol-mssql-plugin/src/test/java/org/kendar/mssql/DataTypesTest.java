package org.kendar.mssql;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("SqlNoDataSourceInspection")
public class DataTypesTest extends MssqlBasicTest {
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
    void dataTypesViaLiterals() throws Exception {
        var c = getProxyConnection();
        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE DT_LIT (" +
                "ID INT PRIMARY KEY NOT NULL," +
                "B BIT," +
                "TI TINYINT," +
                "SI SMALLINT," +
                "BI BIGINT," +
                "DE DECIMAL(10,3)," +
                "FL FLOAT," +
                "RE REAL," +
                "VC VARCHAR(100)," +
                "NV NVARCHAR(100)," +
                "DA DATE," +
                "TM TIME," +
                "DT2 DATETIME2," +
                "UI UNIQUEIDENTIFIER)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("INSERT INTO DT_LIT (ID,B,TI,SI,BI,DE,FL,RE,VC,NV,DA,TM,DT2,UI) VALUES (" +
                "1,1,255,32000,9000000000,123.456,1.5,2.5,'varchar',N'nvarchar'," +
                "'2024-01-15','10:30:00','2024-01-15 10:30:00'," +
                "'12345678-1234-1234-1234-123456789012')");
        stmt.close();

        stmt = c.createStatement();
        var rs = stmt.executeQuery("SELECT * FROM DT_LIT");
        var runned = false;
        while (rs.next()) {
            assertEquals(1, rs.getInt("ID"));
            assertTrue(rs.getBoolean("B"));
            assertEquals(255, rs.getInt("TI"));
            assertEquals(32000, rs.getShort("SI"));
            assertEquals(9000000000L, rs.getLong("BI"));
            assertEquals(0, new BigDecimal("123.456").compareTo(rs.getBigDecimal("DE")));
            assertEquals(1.5, rs.getDouble("FL"), 0.0001);
            assertEquals(2.5, rs.getFloat("RE"), 0.0001);
            assertEquals("varchar", rs.getString("VC"));
            assertEquals("nvarchar", rs.getString("NV"));
            assertEquals("2024-01-15", rs.getDate("DA").toString());
            assertEquals("10:30:00", rs.getTime("TM").toString());
            assertTrue(rs.getTimestamp("DT2").toString().startsWith("2024-01-15 10:30:00"));
            assertEquals("12345678-1234-1234-1234-123456789012",
                    rs.getString("UI").toLowerCase());
            runned = true;
        }
        rs.close();
        stmt.close();
        c.close();
        assertTrue(runned);
    }
}
