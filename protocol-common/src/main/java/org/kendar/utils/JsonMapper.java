package org.kendar.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wrapper to have a single json serializer
 */
public class JsonMapper {
    private static final ObjectMapper mapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public String serializePretty(Object target) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String serialize(Object target) {
        try {
            return mapper.writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T deserialize(Object serialized, Class<T> target) {
        try {
            if (serialized == null) return null;
            if (serialized instanceof String) return mapper.readValue((String) serialized, target);
            if (serialized instanceof JsonNode) return mapper.treeToValue((JsonNode) serialized, target);
            throw new RuntimeException("ERROR");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T deserialize(Object serialized, TypeReference<T> target) {
        try {
            if (serialized == null) return null;
            if (serialized instanceof String) return mapper.readValue((String) serialized, target);
            if (serialized instanceof JsonNode) return mapper.treeToValue((JsonNode) serialized, target);
            throw new RuntimeException("ERROR");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public JsonNode toJsonNode(Object of) {
        try {
            if (of instanceof String) {
                return mapper.readTree((String) of);
            }
            if (of instanceof JsonNode) {
                return (JsonNode) of;
            }
            return mapper.readTree(mapper.writeValueAsString(of));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
