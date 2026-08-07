package org.kendar.mssql.plugins;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcReplayPlugin;
import org.kendar.sql.parser.SqlStringParser;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mssql")
public class MssqlReplayPlugin extends JdbcReplayPlugin {


    private static final SqlStringParser parser = new SqlStringParser("@");

    @TpmConstructor
    public MssqlReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

    @Override
    protected SqlStringParser getParser() {
        return parser;
    }

    @Override
    public String getProtocol() {
        return "mssql";
    }


}
