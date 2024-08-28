package org.kendar.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.mqtt.utils.MqttStorage;
import org.kendar.storage.BaseFileStorage;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MqttFileStorage extends BaseFileStorage<JsonNode, JsonNode> implements MqttStorage {
    private static final Logger log = LoggerFactory.getLogger(MqttFileStorage.class);
    private static final List<String> toAvoid = List.of();
    private final List<StorageItem<JsonNode, JsonNode>> inMemoryDb = new ArrayList<>();
    private final List<StorageItem<JsonNode, JsonNode>> compareData = new ArrayList<>();
    private final List<StorageItem<JsonNode, JsonNode>> outItems = new ArrayList<>();
    private final Object lockObject = new Object();
    private final Object responseLockObject = new Object();
    private boolean initialized = false;
    private List<CompactLine> index;

    public MqttFileStorage(String targetDir) {
        super(targetDir);
    }

    public MqttFileStorage(Path targetDir) {
        super(targetDir);
    }

    private static int getConsumeId(JsonNode output, int consumeId) {
        if (output == null) return 0;
        var data = output.get("data");
        if (data == null) return consumeId;
        var cid = data.get("consumeId");
        if (cid == null) return consumeId;
        return Math.max(cid.asInt(), consumeId);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<JsonNode, JsonNode>>() {
        };
    }

    @Override
    protected boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<JsonNode, JsonNode> item, List<StorageItem<JsonNode, JsonNode>> loadedData) {
        if (useFullData) return false;
        if (cl == null) return false;
        if (cl.getTags() == null || cl.getTags().get("input") == null) {
            return false;
        }
        return toAvoid.contains(cl.getTags().get("input"));
    }

    @Override
    public StorageItem<JsonNode, JsonNode> read(JsonNode node, String type) {
        initializeContent();
        synchronized (lockObject) {
            var item = inMemoryDb.stream()
                    .filter(a -> type.equalsIgnoreCase(a.getType()) &&
                            a.getCaller().equalsIgnoreCase("MQTT")).findFirst();
            var idx = index.stream()
                    .filter(a -> type.equalsIgnoreCase(a.getType()) &&
                            a.getCaller().equalsIgnoreCase("MQTT")).findFirst();

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
    public List<StorageItem<JsonNode, JsonNode>> readResponses(long afterIndex) {
        initializeContent();
        synchronized (responseLockObject) {
            var result = new ArrayList<StorageItem<JsonNode, JsonNode>>();
            for (var item : index.stream().filter(a -> a.getIndex() > afterIndex).collect(Collectors.toList())) {
                if (item.getType().equalsIgnoreCase("RESPONSE")) {
                    var outItem = outItems.stream().filter(a -> a.getIndex() == item.getIndex()).findFirst();
                    if (outItem.isPresent()) {
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

    protected Map<String, String> buildTag(StorageItem<JsonNode, JsonNode> item) {
        var data = new HashMap<String, String>();
        var consumeId = 0;
        data.put("input", null);
        data.put("output", null);
        data.put("consumeId", null);
        if (item.getInput() != null) {
            if (item.getInput().get("type") != null) {
                data.put("input", item.getInput().get("type").textValue());
                consumeId = getConsumeId(item.getOutput(), consumeId);
            }
        }
        if (item.getOutput() != null) {
            if (item.getOutput().get("type") != null) {
                data.put("output", item.getOutput().get("type").textValue());
                consumeId = getConsumeId(item.getOutput(), consumeId);
            }
        }
        if (consumeId > 0) {
            data.put("consumeId", consumeId + "");
        }
        return data;
    }

    @Override
    protected List<StorageItem<JsonNode, JsonNode>> readAllItems() {
        return super.readAllItems();
    }

}
