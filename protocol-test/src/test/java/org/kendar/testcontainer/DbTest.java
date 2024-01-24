package org.kendar.testcontainer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kendar.testcontainer.images.MysqlImage;
import org.kendar.testcontainer.images.PostgreslImage;
import org.kendar.testcontainer.utils.Utils;
import org.testcontainers.containers.Network;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DbTest {
    @Test
    @Disabled("Only callable directly")
    void testMysql() throws Exception {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();

        try (var mySqlContainer = new MysqlImage()) {
            mySqlContainer
                    .withNetwork(network)
                    .withAliases("mysql.sample.test", "mysql.proxy.test")
                    .withInitScript("test", "mysql.sql")
                    .start();
            var connection = DriverManager.getConnection(mySqlContainer.getJdbcUrl(),
                    mySqlContainer.getUserId(), mySqlContainer.getPassword());
            var statement = connection.createStatement();
            var resultset = statement.executeQuery("SELECT DENOMINATION FROM COMPANY");
            while (resultset.next()) {
                assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            }
            resultset.close();
            statement.close();
            connection.close();

        }
    }


    @Test
    @Disabled("Only callable directly")
    void testPostgres() throws Exception {
        var dockerHost = Utils.getDockerHost();
        assertNotNull(dockerHost);
        var network = Network.newNetwork();

        try (var postgresContainer = new PostgreslImage()) {
            postgresContainer
                    .withNetwork(network)
                    .withAliases("postgres.sample.test", "postgres.proxy.test")
                    .withInitScript("test", "postgres.sql")
                    .start();
            var connection = DriverManager.getConnection(postgresContainer.getJdbcUrl(),
                    postgresContainer.getUserId(), postgresContainer.getPassword());
            var statement = connection.createStatement();
            var resultset = statement.executeQuery("SELECT DENOMINATION FROM COMPANY");
            while (resultset.next()) {
                assertEquals("Test Ltd", resultset.getString("DENOMINATION"));
            }
            resultset.close();
            statement.close();
            connection.close();

        }
    }
}
