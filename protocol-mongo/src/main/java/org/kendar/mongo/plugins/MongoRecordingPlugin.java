package org.kendar.mongo.plugins;

import org.kendar.plugins.RecordingPlugin;

public class MongoRecordingPlugin extends RecordingPlugin {


    @Override
    public String getProtocol() {
        return "mongodb";
    }


}
