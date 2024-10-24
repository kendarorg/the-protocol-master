package org.kendar.redis.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.storage.Storage;

public interface Resp3Storage extends Storage<JsonNode, JsonNode> {
}
