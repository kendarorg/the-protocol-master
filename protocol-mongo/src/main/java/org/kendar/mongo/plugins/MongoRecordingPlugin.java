package org.kendar.mongo.plugins;

import org.kendar.plugins.RecordingPlugin;
import org.kendar.plugins.settings.BasicRecordingPluginSettings;

public class MongoRecordingPlugin extends RecordingPlugin<BasicRecordingPluginSettings> {


    @Override
    public String getProtocol() {
        return "mongodb";
    }


}
