package org.kendar.mssql;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SqlNoDataSourceInspection")
public class DataTypesPsTest extends MssqlBasicTest {
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
    void dataTypesViaPreparedStatement() throws Exception {
        var c = getProxyConnection();
        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE DT_PS (" +
                "ID INT PRIMARY KEY NOT NULL," +
                "B BIT," +
                "BI BIGINT," +
                "DE DECIMAL(10,3)," +
                "FL FLOAT," +
                "VC VARCHAR(100)," +
                "NV NVARCHAR(100)," +
                "VB VARBINARY(100)," +
                "DA DATE," +
                "TM TIME," +
                "DT2 DATETIME2)");
        stmt.close();

        var ps = c.prepareStatement("INSERT INTO DT_PS (ID,B,BI,DE,FL,VC,NV,VB,DA,TM,DT2) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)");
        ps.setInt(1, 1);
        ps.setBoolean(2, true);
        ps.setLong(3, 9000000000L);
        ps.setBigDecimal(4, new BigDecimal("123.456"));
        ps.setDouble(5, 1.5);
        ps.setString(6, "varchar");
        ps.setString(7, "nvarchar");
        ps.setBytes(8, new byte[]{1, 2, 3, 4});
        ps.setDate(9, Date.valueOf("2024-01-15"));
        ps.setTime(10, Time.valueOf("10:30:00"));
        ps.setTimestamp(11, Timestamp.valueOf("2024-01-15 10:30:00"));
        assertEquals(1, ps.executeUpdate());
        ps.close();

        var query = c.prepareStatement("SELECT * FROM DT_PS WHERE ID=?");
        query.setInt(1, 1);
        var rs = query.executeQuery();
        var runned = false;
        while (rs.next()) {
            assertEquals(1, rs.getInt("ID"));
            assertTrue(rs.getBoolean("B"));
            assertEquals(9000000000L, rs.getLong("BI"));
            assertEquals(0, new BigDecimal("123.456").compareTo(rs.getBigDecimal("DE")));
            assertEquals(1.5, rs.getDouble("FL"), 0.0001);
            assertEquals("varchar", rs.getString("VC"));
            assertEquals("nvarchar", rs.getString("NV"));
            assertArrayEquals(new byte[]{1, 2, 3, 4}, rs.getBytes("VB"));
            assertEquals("2024-01-15", rs.getDate("DA").toString());
            assertEquals("10:30:00", rs.getTime("TM").toString());
            assertTrue(rs.getTimestamp("DT2").toString().startsWith("2024-01-15 10:30:00"));
            runned = true;
        }
        rs.close();
        query.close();
        c.close();
        assertTrue(runned);
    }
}
