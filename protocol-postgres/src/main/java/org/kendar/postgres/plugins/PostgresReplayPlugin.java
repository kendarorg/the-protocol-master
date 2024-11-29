package org.kendar.postgres.plugins;

import org.kendar.plugins.JdbcReplayPlugin;
import org.kendar.sql.parser.SqlStringParser;

public class PostgresReplayPlugin extends JdbcReplayPlugin {


    private static final SqlStringParser parser = new SqlStringParser("$");
    @Override
    protected SqlStringParser getParser() {
        return parser;
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }


}
