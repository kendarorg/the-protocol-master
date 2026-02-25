package org.kendar.postgres.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicForwardPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresForwardPlugin extends BasicForwardPlugin {


    public PostgresForwardPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
