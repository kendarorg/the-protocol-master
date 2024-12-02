package org.kendar.mysql.plugins;

import org.kendar.plugins.JdbcReportPlugin;

public class MySqlReportPlugin extends JdbcReportPlugin {
    @Override
    public String getProtocol() {
        return "mysql";
    }
}
