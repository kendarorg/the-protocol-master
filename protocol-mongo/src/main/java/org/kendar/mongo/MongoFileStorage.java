package org.kendar.mongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.mongo.utils.MongoStorage;
import org.kendar.storage.BaseFileStorage;
import org.kendar.storage.StorageItem;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class MongoFileStorage extends BaseFileStorage<JsonNode, JsonNode> implements MongoStorage {
    private ConcurrentHashMap<Long, StorageItem<JsonNode, JsonNode>> inMemoryDb = new ConcurrentHashMap<>();
    private boolean initialized = false;
    private Object lockObject = new Object();

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
}
