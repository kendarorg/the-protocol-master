package org.kendar.mysql.plugins;

import org.kendar.plugins.JdbcMockPlugin;

public class MySqlMockPlugin extends JdbcMockPlugin {
    @Override
    public String getProtocol() {
        return "mysql";
    }

    @Override
    protected String getSqlStringParserSeparator() {
        return "?";
    }
}
