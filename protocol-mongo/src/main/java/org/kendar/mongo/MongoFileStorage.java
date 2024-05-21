package org.kendar.mongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.mongo.utils.MongoStorage;
import org.kendar.storage.BaseFileStorage;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MongoFileStorage extends BaseFileStorage<JsonNode, JsonNode> implements MongoStorage {
    private static final Logger log = LoggerFactory.getLogger(MongoFileStorage.class);
    private final ConcurrentHashMap<Long, StorageItem<JsonNode, JsonNode>> inMemoryDb = new ConcurrentHashMap<>();
    private final Object lockObject = new Object();
    private boolean initialized = false;

    public MongoFileStorage(String targetDir) {
        super(targetDir);
    }

    public MongoFileStorage(Path targetDir) {
        super(targetDir);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<JsonNode, JsonNode>>() {
        };
    }

    @Override
    public StorageItem<JsonNode, JsonNode> read(JsonNode node, String type) {
        if (!initialized) {
            for (var item : readAllItems()) {
                inMemoryDb.put(item.getIndex(), item);
            }
            initialized = true;
        }
        synchronized (lockObject) {
            var item = inMemoryDb.values().stream()
                    .filter(a -> {
                        var req = (JsonNode) a.getInput();
                        return
//                    req.getQuery().equalsIgnoreCase(query) &&
//                            type.equalsIgnoreCase(a.getType()) &&
//                            parameterValues.size()==req.getParameterValues().size() &&
                                type.equalsIgnoreCase(a.getType()) &&
                                        a.getCaller().equalsIgnoreCase("MONGODB");
                    }).findFirst();
            if (item.isPresent()) {
                inMemoryDb.remove(item.get().getIndex());
                return item.get();
            }

            return null;
        }
    }

    @Override
    public List<StorageItem<JsonNode, JsonNode>> readResponses(long afterIndex) {
        throw new RuntimeException("PUSH NOT IMPLEMENTED");
    }

    @Override
    protected boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<JsonNode, JsonNode> item, List<StorageItem<JsonNode, JsonNode>> loadedData) {
        return false;
    }

    @Override
    protected Map<String, String> buildTag(StorageItem<JsonNode, JsonNode> item) {
        return null;
    }
}
