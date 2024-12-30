package org.kendar.postgres.plugins;

import org.kendar.annotations.di.TpmService;
import org.kendar.plugins.JdbcRewritePlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresRewritePlugin extends JdbcRewritePlugin {
    public PostgresRewritePlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
