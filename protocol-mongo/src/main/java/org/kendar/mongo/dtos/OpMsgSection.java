package org.kendar.mongo.dtos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kendar.utils.JsonMapper;

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

    public Map<String, Object> serialize(JsonMapper mapper) {
        var result = new HashMap<String, Object>();
        if (identifier != null && !identifier.isEmpty()) {
            result.put("identifier", identifier);
        }
        var list = new ArrayList<JsonNode>();
        for (var item : documents) {
            var doc = (ObjectNode) mapper.toJsonNode(item);
            doc.remove("lsid");
            doc.remove("$clusterTime");
            doc.remove("apiVersion");
            list.add(doc);
        }
        result.put("documents", list);
        return result;

    }

    protected void doDeserialize(JsonNode toDeserialize, JsonMapper mapper) {
        var jnIdentifier = toDeserialize.get("identifier");
        if (jnIdentifier != null) {
            identifier = jnIdentifier.asText();
        }
        documents = new ArrayList<>();
        var jnDocuments = toDeserialize.get("documents");
        if (jnDocuments != null && !jnDocuments.isEmpty()) {
            for (var i = 0; i < jnDocuments.size(); i++) {
                var doc = jnDocuments.get(i);
                documents.add(mapper.serialize(doc));
            }
        }
    }
}
