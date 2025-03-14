package org.kendar.apis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import java.io.IOException;

public class JsonSmile {
    private static final ObjectMapper smileMapper = new ObjectMapper(new SmileFactory());
    private static final ObjectMapper mapper = new ObjectMapper();

    public static byte[] jsonToSmile(String jsonValue) throws JsonProcessingException {
        JsonNode actualObj = mapper.readTree(jsonValue);
        return smileMapper.writeValueAsBytes(actualObj);
    }

    public static byte[] jsonToSmile(JsonNode actualObj) throws JsonProcessingException {
        return smileMapper.writeValueAsBytes(actualObj);
    }

    public static JsonNode smileToJSON(byte[] smileBytes) throws IOException {
        return smileMapper.readValue(smileBytes, JsonNode.class);
    }
}
