package org.kendar.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapper {
    private static final ObjectMapper mapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String serialize(Object target) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T deserialize(String serialized, Class<T> target) {
        try {
            return mapper.readValue(serialized, target);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public <T> T deserialize(String serialized, TypeReference target) {
        try {
            return (T)mapper.readValue(serialized, target);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
