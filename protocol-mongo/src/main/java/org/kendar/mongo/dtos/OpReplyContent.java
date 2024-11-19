package org.kendar.mongo.dtos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bson.BsonDocument;
import org.kendar.buffers.BBuffer;
import org.kendar.mongo.fsm.OpCodes;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpReplyContent extends BaseMessageData {
    private List<String> documents = new ArrayList<>();
    private int cursorId;

    public OpReplyContent() {
        setOpCode(OpCodes.OP_REPLY);
    }

    public OpReplyContent(int flags, int requestId, int responseId) {
        super(flags, requestId, responseId, OpCodes.OP_REPLY);
    }

    public List<String> getDocuments() {
        return documents;
    }

    public void write(BBuffer responseBuffer) {
        super.write(responseBuffer);

        responseBuffer.writeLong(cursorId);
        responseBuffer.writeInt(0);
        responseBuffer.writeInt(documents.size());
        for (String document : documents) {
            var doc = BsonDocument.parse(document);
            byte[] query = toBytes(doc);
            responseBuffer.write(query);
        }
        var position = responseBuffer.getPosition();
        responseBuffer.writeInt(position, 0);
    }

    @Override
    protected void serialize(HashMap<String, Object> dataMap, JsonMapper mapper) {
        dataMap.put("cursorId", cursorId);

        var list = new ArrayList<JsonNode>();
        for (var item : documents) {
            list.add(mapper.toJsonNode(item));
        }
        dataMap.put("documents", list);
    }

    @Override
    public void doDeserialize(JsonNode toDeserialize, JsonMapper mapper) {
        super.doDeserialize(toDeserialize, mapper);

        var jnCursorId = toDeserialize.get("cursorId");
        if (jnCursorId != null) {
            cursorId = jnCursorId.asInt();
        }
        documents = new ArrayList<>();
        var jnDocuments = toDeserialize.get("documents");
        if (jnDocuments != null && !jnDocuments.isEmpty()) {
            for (var i = 0; i < jnDocuments.size(); i++) {
                var doc = (ObjectNode) jnDocuments.get(i);
                doc.remove("lsid");
                doc.remove("$clusterTime");
                doc.remove("apiVersion");
                documents.add(mapper.serialize(doc));
            }
        }
    }

    public int getCursorId() {
        return cursorId;
    }

    public void setCursorId(int cursorId) {
        this.cursorId = cursorId;
    }
}
