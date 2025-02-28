package org.kendar.postgres.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "postgres")
public class PostgresNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public PostgresNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "postgres";
    }
}
