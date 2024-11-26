package org.kendar.postgres.plugins;

import org.kendar.plugins.JdbcReplayPlugin;

public class PostgresReplayPlugin extends JdbcReplayPlugin {

    @Override
    public String getProtocol() {
        return "postgres";
    }


}
