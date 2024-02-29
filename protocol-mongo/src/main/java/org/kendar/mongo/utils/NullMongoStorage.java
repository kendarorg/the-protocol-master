package org.kendar.mongo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;
import org.kendar.storage.StorageItem;

public class NullMongoStorage implements Storage<JsonNode, JsonNode>, MongoStorage {
    @Override
    public void initialize() {

    }

    @Override
    public StorageItem<JsonNode, JsonNode> read(JsonNode node, String type) {
        return null;
    }

    @Override
    public void write(JsonNode request, JsonNode response, long durationMs, String type, String caller) {

    }
}
