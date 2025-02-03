package org.kendar.command;

import org.kendar.di.annotations.TpmService;
import org.kendar.mysql.MySqlProtocolSettings;

@TpmService
public class MySQLRunner extends JdbcRunner {
    public MySQLRunner() {
        super("mysql");
    }

    @Override
    public Class<?> getSettingsClass() {
        return MySqlProtocolSettings.class;
    }
}
