package org.kendar.mongo.dtos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpMsgSection {
    private String identifier;
    private List<String> documents = new ArrayList<>();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public void setDocuments(List<String> documents) {
        this.documents = documents;
    }

    public Map<String, Object> serialize(ObjectMapper mapper) {
        try {
            var result = new HashMap<String, Object>();
            if (identifier != null && !identifier.isEmpty()) {
                result.put("identifier", identifier);
            }
            var list = new ArrayList<JsonNode>();
            for (var item : documents) {
                var doc = (ObjectNode) mapper.readTree(item);
                doc.remove("lsid");
                doc.remove("$clusterTime");
                doc.remove("apiVersion");
                list.add(doc);
            }
            result.put("documents", list);
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    protected void doDeserialize(JsonNode toDeserialize, ObjectMapper mapper) {
        var jnIdentifier = toDeserialize.get("identifier");
        if (jnIdentifier != null) {
            identifier = jnIdentifier.asText();
        }
        documents = new ArrayList<>();
        var jnDocuments = toDeserialize.get("documents");
        if (jnDocuments != null && jnDocuments.size() > 0) {
            for (var i = 0; i < jnDocuments.size(); i++) {
                var doc = jnDocuments.get(i);
                try {
                    documents.add(mapper.writeValueAsString(doc));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
