package org.kendar.mssql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mssql")
public class MssqlRestPluginsPlugin extends BasicRestPluginsPlugin {
    public MssqlRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mssql";
    }
}
