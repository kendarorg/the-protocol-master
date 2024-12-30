package org.kendar.postgres.plugins;

import org.kendar.annotations.TpmConstructor;
import org.kendar.annotations.di.TpmService;
import org.kendar.plugins.JdbcReplayPlugin;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresReplayPlugin extends JdbcReplayPlugin {


    private static final SqlStringParser parser = new SqlStringParser("$");
    @TpmConstructor
    public PostgresReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

    @Override
    protected SqlStringParser getParser() {
        return parser;
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }


}
