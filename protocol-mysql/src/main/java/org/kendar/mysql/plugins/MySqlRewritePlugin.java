package org.kendar.mysql.plugins;

import org.kendar.plugins.JdbcRewritePlugin;

public class MySqlRewritePlugin extends JdbcRewritePlugin {
    @Override
    public String getProtocol() {
        return "mysql";
    }
}
