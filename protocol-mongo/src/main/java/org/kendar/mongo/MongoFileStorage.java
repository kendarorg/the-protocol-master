package org.kendar.mongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.mongo.utils.MongoStorage;
import org.kendar.storage.BaseStorage;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.StorageRepository;

import java.util.List;
import java.util.Map;

public class MongoFileStorage extends BaseStorage<JsonNode, JsonNode> implements MongoStorage {


    public MongoFileStorage(StorageRepository<JsonNode, JsonNode> repository) {
        super(repository);
    }

    @Override
    public String getCaller() {
        return "MONGODB";
    }

    @Override
    public TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<JsonNode, JsonNode>>() {
        };
    }

    @Override
    public boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<JsonNode, JsonNode> item, List<StorageItem<JsonNode, JsonNode>> loadedData) {
        return false;
    }

    @Override
    public Map<String, String> buildTag(StorageItem<JsonNode, JsonNode> item) {
        return Map.of();
    }

    @Override
    public StorageItem<JsonNode, JsonNode> read(JsonNode node, String type) {
        var siQuery = new CallItemsQuery();
        siQuery.setCaller(getCaller());
        siQuery.setType(type);
        return read(siQuery);
    }

    @Override
    public List<StorageItem<JsonNode, JsonNode>> readResponses(long afterIndex) {
        return List.of();
    }
}
