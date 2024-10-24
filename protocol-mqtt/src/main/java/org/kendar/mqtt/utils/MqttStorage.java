package org.kendar.mqtt.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;

public interface MqttStorage extends Storage<JsonNode, JsonNode> {
}
