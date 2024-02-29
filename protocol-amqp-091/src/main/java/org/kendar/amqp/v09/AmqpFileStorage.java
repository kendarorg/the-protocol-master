package org.kendar.amqp.v09;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.amqp.v09.utils.AmqpStorage;
import org.kendar.storage.BaseFileStorage;
import org.kendar.storage.StorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AmqpFileStorage extends BaseFileStorage<JsonNode, JsonNode> implements AmqpStorage {
    private static final Logger log = LoggerFactory.getLogger(AmqpFileStorage.class);
    private final List<StorageItem<JsonNode, JsonNode>> inMemoryDb = new ArrayList<>();
    private final List<StorageItem<JsonNode, JsonNode>> compareData = new ArrayList<>();
    private final List<StorageItem<JsonNode, JsonNode>> outItems = new ArrayList<>();
    private final Object lockObject = new Object();
    private final Object responseLockObject = new Object();
    private boolean initialized = false;

    public AmqpFileStorage(String targetDir) {
        super(targetDir);
    }

    public AmqpFileStorage(Path targetDir) {
        super(targetDir);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<JsonNode, JsonNode>>() {
        };
    }

    @Override
    public StorageItem<JsonNode, JsonNode> read(JsonNode node, String type) {
        initializeContent();
        synchronized (lockObject) {
            var item = inMemoryDb.stream()
                    .filter(a -> type.equalsIgnoreCase(a.getType()) &&
                            a.getCaller().equalsIgnoreCase("AMQP")).findFirst();
            if (item.isPresent()) {
                inMemoryDb.remove(item.get());
                return item.get();
            }

            return null;
        }
    }

    private void initializeContent() {
        if (!initialized) {
            for (var item : readAllItems()) {
                compareData.add(item);
                if (item.getType().equalsIgnoreCase("RESPONSE")) {
                    outItems.add(item);
                    continue;
                }
                inMemoryDb.add(item);
            }
            initialized = true;
        }
    }

    @Override
    public List<StorageItem<JsonNode, JsonNode>> readResponses(long afterIndex) {
        initializeContent();
        synchronized (responseLockObject) {
            var result = new ArrayList<StorageItem<JsonNode, JsonNode>>();
            for (var item : compareData.stream().filter(a -> a.getIndex() > afterIndex).collect(Collectors.toList())) {
                if (item.getType().equalsIgnoreCase("RESPONSE")) {
                    if (outItems.contains(item)) {
                        result.add(item);
                        log.trace("[SERVER][CB] After: " + afterIndex + " Index: " + item.getIndex() + " Type: " + item.getOutput().get("type").textValue());
                        outItems.remove(item);
                    }
                } else {
                    break;
                }
            }
            return result;
        }
    }
}
