package org.kendar.mssql;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.sql.jdbc.JdbcProxy;

@TpmService
public class MssqlProxy extends JdbcProxy {
    @TpmConstructor
    public MssqlProxy(MssqlProtocolSettings settings) {
        super(settings);
    }

    public MssqlProxy(String driver) {
        super(driver);
    }

    public MssqlProxy(String driver, String connectionString, String forcedSchema, String login, String password) {
        super(driver, connectionString, forcedSchema, login, password);
    }

    @Override
    protected String getDefaultDriver() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }
}
