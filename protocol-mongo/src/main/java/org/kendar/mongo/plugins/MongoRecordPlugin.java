package org.kendar.mongo.plugins;

import org.kendar.di.annotations.TpmService;
import org.kendar.mongo.dtos.BaseMessageData;
import org.kendar.plugins.BasicRecordPlugin;
import org.kendar.plugins.settings.BasicRecordPluginSettings;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;

@TpmService(tags = "mongodb")
public class MongoRecordPlugin extends BasicRecordPlugin<BasicRecordPluginSettings> {
    public MongoRecordPlugin(JsonMapper mapper, StorageRepository storage,
                             MultiTemplateEngine resolversFactory, SimpleParser parser) {
        super(mapper, storage, resolversFactory, parser);
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
