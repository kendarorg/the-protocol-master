package org.kendar.mssql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicForwardPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mssql")
public class MssqlForwardPlugin extends BasicForwardPlugin {
    public MssqlForwardPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mssql";
    }
}
