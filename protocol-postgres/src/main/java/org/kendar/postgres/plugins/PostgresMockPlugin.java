package org.kendar.postgres.plugins;

import org.kendar.annotations.TpmConstructor;
import org.kendar.annotations.di.TpmService;
import org.kendar.plugins.JdbcMockPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresMockPlugin extends JdbcMockPlugin {


    @TpmConstructor
    public PostgresMockPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
