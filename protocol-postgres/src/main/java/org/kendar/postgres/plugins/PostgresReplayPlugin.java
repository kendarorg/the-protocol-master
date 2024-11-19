package org.kendar.postgres.plugins;

import org.kendar.plugins.JdbcReplayingPlugin;

public class PostgresReplayPlugin extends JdbcReplayingPlugin {

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
