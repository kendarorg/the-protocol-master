package org.kendar.mssql;

import org.junit.jupiter.api.Test;
import org.kendar.mssql.plugins.MssqlReplayPlugin;
import org.kendar.plugins.settings.BasicReplayPluginSettings;
import org.kendar.settings.ByteProtocolSettingsWithLogin;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.FileStorageRepository;
import org.kendar.tcpserver.NettyServer;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("SqlNoDataSourceInspection")
public class ReplayerTest {
    protected static final int FAKE_PORT = 1436;

    @Test
    void simpleReplayTest() throws Exception {
        var baseProtocol = new MssqlProtocol(FAKE_PORT);
        var proxy = new MssqlProxy("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        var storage = new FileStorageRepository(Path.of("src",
                "test", "resources", "replay"));
        storage.initialize();

        var gs = new GlobalSettings();
        var mapper = new JsonMapper();
        var pl = new MssqlReplayPlugin(mapper, storage).initialize(gs, new ByteProtocolSettingsWithLogin(), new BasicReplayPluginSettings());
        proxy.setPluginHandlers(List.of(pl));
        pl.setActive(true);
        baseProtocol.setProxy(proxy);
        baseProtocol.initialize();
        var protocolServer = new NettyServer(baseProtocol);

        protocolServer.start();
        Sleeper.sleep(5000, protocolServer::isRunning);

        Connection c;
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        c = DriverManager
                .getConnection(String.format(
                                "jdbc:sqlserver://127.0.0.1:%d;encrypt=false;trustServerCertificate=true",
                                FAKE_PORT),
                        "sa", "password");

        var runned = false;
        var stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE COMPANY_2 " +
                "(ID INT PRIMARY KEY NOT NULL," +
                " DENOMINATION VARCHAR(255) NOT NULL, " +
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
        protocolServer.stop();
        Sleeper.sleep(100);
    }
}
