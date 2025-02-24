package org.kendar.postgres;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.sql.jdbc.JdbcProxy;
import org.kendar.sql.jdbc.settings.JdbcProtocolSettings;

@TpmService
public class PostgresProxy extends JdbcProxy {
    @TpmConstructor
    public PostgresProxy(JdbcProtocolSettings settings) {
        super(settings);
    }

    public PostgresProxy(String driver) {
        super(driver);
    }

    public PostgresProxy(String driver, String connectionString, String forcedSchema, String login, String password) {
        super(driver, connectionString, forcedSchema, login, password);
    }

    @Override
    protected String getDefaultDriver() {
        return "org.postgresql.Driver";
    }
}
