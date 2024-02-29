package org.kendar.mongo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;
import org.kendar.storage.StorageItem;

public interface MongoStorage extends Storage<JsonNode, JsonNode> {
    void initialize();

    StorageItem<JsonNode, JsonNode> read(JsonNode node, String type);
}
