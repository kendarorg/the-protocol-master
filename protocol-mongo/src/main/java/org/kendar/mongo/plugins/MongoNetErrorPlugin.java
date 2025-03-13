package org.kendar.mongo.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicNetworkErrorPlugin;
import org.kendar.plugins.settings.NetworkErrorPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mongodb")
public class MongoNetErrorPlugin extends BasicNetworkErrorPlugin<NetworkErrorPluginSettings> {
    public MongoNetErrorPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mongodb";
    }
}
