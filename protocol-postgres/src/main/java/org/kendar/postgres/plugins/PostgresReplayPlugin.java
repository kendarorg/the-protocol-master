package org.kendar.postgres.plugins;

import org.kendar.plugins.JdbcReplayingPlugin;
import org.kendar.sql.parser.SqlStringParser;

public class PostgresReplayPlugin extends JdbcReplayingPlugin {

    @Override
    public String getProtocol() {
        return "postgres";
    }


}
