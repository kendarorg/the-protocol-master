package org.kendar.mongo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.StorageItem;
import org.kendar.storage.StorageRoot;

public interface MongoStorage extends StorageRoot<JsonNode,JsonNode> {
    void initialize();
    StorageItem<JsonNode, JsonNode> read(JsonNode node,String type);
}
