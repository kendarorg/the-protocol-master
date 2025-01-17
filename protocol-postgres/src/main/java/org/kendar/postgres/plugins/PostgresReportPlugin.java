package org.kendar.postgres.plugins;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcReportPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresReportPlugin extends JdbcReportPlugin {


    @TpmConstructor
    public PostgresReportPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
