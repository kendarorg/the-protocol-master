package org.kendar.mongo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;
import org.kendar.storage.StorageItem;

import java.util.List;

public class NullMongoStorage implements Storage<JsonNode, JsonNode>, MongoStorage {
    @Override
    public void initialize() {

    }

    @Override
    public Storage<JsonNode, JsonNode> withFullData() {
        return this;
    }

    @Override
    public StorageItem<JsonNode, JsonNode> read(JsonNode node, String type) {
        return null;
    }

    @Override
    public List<StorageItem<JsonNode, JsonNode>> readResponses(long afterIndex) {
        throw new RuntimeException("PUSH NOT IMPLEMENTED");
    }

    @Override
    public void write(int connectionId, JsonNode request, JsonNode response, long durationMs, String type, String caller) {

    }

    @Override
    public void write(long index, int connectionId, JsonNode request, JsonNode response, long durationMs, String type, String caller) {

    }

    @Override
    public void optimize() {

    }

    @Override
    public long generateIndex() {
        return 0;
    }
}
