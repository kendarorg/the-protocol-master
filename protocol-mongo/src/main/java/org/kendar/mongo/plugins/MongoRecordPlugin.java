package org.kendar.mongo.plugins;

import org.kendar.plugins.RecordPlugin;
import org.kendar.plugins.settings.BasicRecordPluginSettings;

public class MongoRecordPlugin extends RecordPlugin<BasicRecordPluginSettings> {


    @Override
    public String getProtocol() {
        return "mongodb";
    }


}
