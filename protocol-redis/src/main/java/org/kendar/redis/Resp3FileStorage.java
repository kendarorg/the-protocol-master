package org.kendar.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.redis.utils.Resp3Storage;
import org.kendar.storage.BaseFileStorage;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Resp3FileStorage extends BaseFileStorage<JsonNode, JsonNode> implements Resp3Storage {
    private static final Logger log = LoggerFactory.getLogger(Resp3FileStorage.class);

    private static final List<String> toAvoid = List.of();
    private final List<StorageItem<JsonNode, JsonNode>> inMemoryDb = new ArrayList<>();
    private final List<StorageItem<JsonNode, JsonNode>> compareData = new ArrayList<>();
    private final List<StorageItem<JsonNode, JsonNode>> outItems = new ArrayList<>();
    private final Object lockObject = new Object();
    private final Object responseLockObject = new Object();
    private final JsonMapper mapper = new JsonMapper();
    private boolean initialized = false;
    private List<CompactLine> index;

    public Resp3FileStorage(String targetDir) {
        super(targetDir);
    }

    public Resp3FileStorage(Path targetDir) {
        super(targetDir);
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

            index = retrieveIndexFile();
            initialized = true;
        }
    }

    @Override
    public StorageItem<JsonNode, JsonNode> read(JsonNode node, String type) {
        initializeContent();
        synchronized (lockObject) {
            var item = inMemoryDb.stream()
                    .filter(a -> type.equalsIgnoreCase(a.getType()) &&
                            a.getCaller().equalsIgnoreCase("RESP3")).findFirst();
            var idx = index.stream()
                    .filter(a -> type.equalsIgnoreCase(a.getType()) &&
                            a.getCaller().equalsIgnoreCase("RESP3")).findFirst();

            CompactLine cl = null;
            if (idx.isPresent()) {
                cl = idx.get();
            }
            var shouldNotSave = shouldNotSave(cl, null, null, null);
            if (item.isPresent() && !shouldNotSave) {

                log.debug("[SERVER][REPFULL]  " + item.get().getIndex() + ":" + item.get().getType());
                inMemoryDb.remove(item.get());
                if (idx.isPresent()) {
                    index.remove(idx.get());
                }
                return item.get();
            }

            if (idx.isPresent()) {
                log.debug("[SERVER][REPSHRT] " + idx.get().getIndex() + ":" + idx.get().getType());
                index.remove(idx.get());
                var sti = new StorageItem<JsonNode, JsonNode>();
                sti.setIndex(idx.get().getIndex());
                return sti;
            }


            return null;
        }
    }

    @Override
    public List<StorageItem<JsonNode, JsonNode>> readResponses(long afterIndex) {
        initializeContent();
        synchronized (responseLockObject) {
            var result = new ArrayList<StorageItem<JsonNode, JsonNode>>();
            for (var item : index.stream().filter(a -> a.getIndex() > afterIndex).collect(Collectors.toList())) {
                if (item.getType().equalsIgnoreCase("RESPONSE")) {
                    var outItem = outItems.stream().filter(a -> a.getIndex() == item.getIndex()).findFirst();
                    if (!outItem.isEmpty()) {
                        result.add(outItem.get());
                        log.debug("[SERVER][CB] After: " + afterIndex + " Index: " + item.getIndex() + " Type: " +
                                outItem.get().getOutput().get("type").textValue());
                        outItems.remove(outItem.get());
                    }
                } else {
                    break;
                }
            }
            return result;
        }
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<JsonNode, JsonNode>>() {
        };
    }

    @Override
    protected boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<JsonNode, JsonNode> item, List<StorageItem<JsonNode, JsonNode>> loadedData) {
        return false;
    }

    @Override
    protected Map<String, String> buildTag(StorageItem<JsonNode, JsonNode> item) {
        return Map.of();
    }
}
