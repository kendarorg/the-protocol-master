package org.kendar.mongo.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicLatencyPlugin;
import org.kendar.plugins.settings.LatencyPluginSettings;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mongodb")
public class MongoLatencyPlugin extends BasicLatencyPlugin<LatencyPluginSettings> {
    public MongoLatencyPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mongodb";
    }

}
