package org.kendar.postgres.plugins;

import org.kendar.plugins.BasicJdbcReplayingPlugin;

public class PostgresReplayPlugin extends BasicJdbcReplayingPlugin {

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
