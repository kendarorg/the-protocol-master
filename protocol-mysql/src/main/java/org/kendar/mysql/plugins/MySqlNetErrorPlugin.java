package org.kendar.mysql.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mysql")
public class MySqlNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public MySqlNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mysql";
    }
}
