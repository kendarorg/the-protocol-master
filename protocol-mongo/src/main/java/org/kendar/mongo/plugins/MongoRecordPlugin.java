package org.kendar.mongo.plugins;

import org.kendar.mongo.dtos.BaseMessageData;
import org.kendar.plugins.RecordPlugin;
import org.kendar.plugins.settings.BasicRecordPluginSettings;

public class MongoRecordPlugin extends RecordPlugin<BasicRecordPluginSettings> {
    @Override
    protected Object getData(Object of) {
        if (of instanceof BaseMessageData) {
            return ((BaseMessageData) of).serialize();
        }
        return of;
    }

    @Override
    public String getProtocol() {
        return "mongodb";
    }
}
