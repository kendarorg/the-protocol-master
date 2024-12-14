package org.kendar.postgres.plugins;

import org.kendar.plugins.JdbcReportPlugin;

public class PostgresReportPlugin extends JdbcReportPlugin {
    @Override
    public String getProtocol() {
        return "postgres";
    }
}
