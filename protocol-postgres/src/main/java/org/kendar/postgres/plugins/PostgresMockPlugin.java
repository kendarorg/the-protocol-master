package org.kendar.postgres.plugins;

import org.kendar.plugins.JdbcMockPlugin;

public class PostgresMockPlugin extends JdbcMockPlugin {
    @Override
    public String getProtocol() {
        return "postgres";
    }
    @Override
    protected String getSqlStringParserSeparator() {
        return "$";
    }
}
