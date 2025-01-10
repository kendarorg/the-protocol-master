package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.JdbcRewritePlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlRewritePlugin extends JdbcRewritePlugin {
    public MySqlRewritePlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
