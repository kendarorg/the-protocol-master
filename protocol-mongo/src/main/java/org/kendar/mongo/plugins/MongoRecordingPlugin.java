package org.kendar.mongo.plugins;

import org.kendar.filters.BasicRecordingPlugin;

public class MongoRecordingPlugin extends BasicRecordingPlugin {


    @Override
    public String getProtocol() {
        return "mongodb";
    }


}
