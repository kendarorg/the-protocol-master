package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlRestPluginsPlugin extends BasicRestPluginsPlugin {
    public MySqlRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
