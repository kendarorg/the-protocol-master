package org.kendar.mongo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;

public interface MongoStorage extends Storage<JsonNode, JsonNode> {
}
