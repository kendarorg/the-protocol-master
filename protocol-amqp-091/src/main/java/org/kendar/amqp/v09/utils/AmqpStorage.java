package org.kendar.amqp.v09.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;
import org.kendar.storage.StorageItem;

import java.util.List;

public interface AmqpStorage extends Storage<JsonNode, JsonNode> {
}
