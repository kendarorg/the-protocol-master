package org.kendar.ui.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.List;

public class RecordingSearchResult {
    private ArrayNode rows;
    private List<String> fields = new ArrayList<String>();


    public String convert(JsonNode jsonNode) {
        if (jsonNode.isBigDecimal()) {
            return jsonNode.decimalValue().toPlainString();
        } else if (jsonNode.isArray() || jsonNode.isObject()) {
            return new JsonMapper().serialize(jsonNode);
        }
        return jsonNode.asText();
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public ArrayNode getRows() {
        return rows;
    }

    public void setRows(ArrayNode rows) {
        this.rows = rows;
    }
}
