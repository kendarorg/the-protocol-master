package org.kendar.mqtt.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;
import org.kendar.storage.StorageItem;

import java.util.List;

public interface MqttStorage extends Storage<JsonNode, JsonNode> {
    StorageItem<JsonNode, JsonNode> read(JsonNode node, String type);

    List<StorageItem<JsonNode, JsonNode>> readResponses(long afterIndex);
}
