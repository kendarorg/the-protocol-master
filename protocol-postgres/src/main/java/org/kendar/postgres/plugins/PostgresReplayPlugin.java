package org.kendar.postgres.plugins;

import org.kendar.filters.BasicJdbcReplayingPlugin;

public class PostgresReplayPlugin extends BasicJdbcReplayingPlugin {

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
