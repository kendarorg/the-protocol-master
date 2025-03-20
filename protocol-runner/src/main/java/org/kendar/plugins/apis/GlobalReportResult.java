package org.kendar.plugins.apis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.List;

public class GlobalReportResult {
    private List<String> fields = new ArrayList<String>();
    private ArrayNode rows = null;

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

    public String convert(JsonNode jsonNode) {
        if (jsonNode.isBigDecimal()) {
            return jsonNode.decimalValue().toPlainString();
        } else if (jsonNode.isArray() || jsonNode.isObject()) {
            return new JsonMapper().serialize(jsonNode);
        }
        return jsonNode.asText();
    }
}
