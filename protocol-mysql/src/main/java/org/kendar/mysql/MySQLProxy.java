package org.kendar.mysql;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.sql.jdbc.JdbcProxy;

@TpmService
public class MySQLProxy extends JdbcProxy {
    @TpmConstructor
    public MySQLProxy(MySqlProtocolSettings settings) {
        super(settings);
    }

    public MySQLProxy(String driver) {
        super(driver);
    }

    public MySQLProxy(String driver, String connectionString, String forcedSchema, String login, String password) {
        super(driver, connectionString, forcedSchema, login, password);
    }

    @Override
    protected String getDefaultDriver() {
        return "com.mysql.cj.jdbc.Driver";
    }
}
