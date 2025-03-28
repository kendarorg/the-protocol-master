package org.kendar.mongo.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicRestPluginsPlugin;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mongodb")
public class MongoRestPluginsPlugin extends BasicRestPluginsPlugin {
    public MongoRestPluginsPlugin(JsonMapper mapper) {
        super(mapper);
    }

    @Override
    public String getProtocol() {
        return "mongodb";
    }
}
