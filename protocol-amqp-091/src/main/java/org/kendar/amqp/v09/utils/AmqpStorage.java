package org.kendar.amqp.v09.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;

public interface AmqpStorage extends Storage<JsonNode, JsonNode> {
}
