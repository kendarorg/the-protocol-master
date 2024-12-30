package org.kendar.mongo.plugins;

import org.kendar.annotations.di.TpmService;
import org.kendar.mongo.dtos.BaseMessageData;
import org.kendar.plugins.RecordPlugin;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

@TpmService(tags = "mongodb")
public class MongoRecordPlugin extends RecordPlugin<BasicRecordPluginSettings> {
    public MongoRecordPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

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
