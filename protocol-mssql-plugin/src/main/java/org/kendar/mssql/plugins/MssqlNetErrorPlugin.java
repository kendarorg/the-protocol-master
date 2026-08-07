package org.kendar.mssql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

@Extension
@TpmService(tags = "mssql")
public class MssqlNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public MssqlNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mssql";
    }
}
