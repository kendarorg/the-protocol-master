package org.kendar.postgres.plugins;

import org.kendar.plugins.JdbcRewritePlugin;

public class PostgresRewritePlugin extends JdbcRewritePlugin {
    @Override
    public String getProtocol() {
        return "postgres";
    }
}
