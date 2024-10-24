package org.kendar.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.redis.utils.Resp3Storage;
import org.kendar.storage.BaseStorage;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.StorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class Resp3FileStorage extends BaseStorage<JsonNode, JsonNode> implements Resp3Storage {
    private static final Logger log = LoggerFactory.getLogger(Resp3FileStorage.class);

    @Override
    public String getCaller() {
        return "RESP3";
    }

    public Resp3FileStorage(StorageRepository<JsonNode, JsonNode> repository) {
        super(repository);
    }


    @Override
    public StorageItem<JsonNode, JsonNode> read(JsonNode node, String type) {
        var siQuery = new CallItemsQuery();
        siQuery.setCaller(getCaller());
        return read(siQuery);
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

}
