package org.kendar.mongo.dtos;

import com.fasterxml.jackson.databind.JsonNode;
import org.bson.BsonDocument;
import org.kendar.buffers.BBuffer;
import org.kendar.mongo.fsm.OpCodes;
import org.kendar.utils.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpMsgContent extends BaseMessageData {
    private List<OpMsgSection> sections = new ArrayList<>();

    public OpMsgContent() {

    }

    public OpMsgContent(int flags, int requestId, int responseId) {
        super(flags, requestId, responseId, OpCodes.OP_MSG);
    }

    public List<OpMsgSection> getSections() {
        return sections;
    }

    public void setSections(List<OpMsgSection> sections) {
        this.sections = sections;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        super.write(resultBuffer);
        for (var section : sections) {
            if (section.getIdentifier() == null || section.getIdentifier().isEmpty()) {
                resultBuffer.write((byte) 0x00);
                var doc = section.getDocuments().get(0);
                var bsonDoc = BsonDocument.parse(doc);
                var docBytes = toBytes(bsonDoc);
                resultBuffer.write(docBytes);
            } else {
                resultBuffer.write((byte) 0x01);
                var blockPos = resultBuffer.getPosition();
                resultBuffer.write(section.getIdentifier().getBytes(StandardCharsets.UTF_8));
                for (var doc : section.getDocuments()) {
                    var bsonDoc = BsonDocument.parse(doc);
                    resultBuffer.write(toBytes(bsonDoc));
                }
                var blockLen = resultBuffer.getPosition() - blockPos;
                resultBuffer.writeInt(blockPos, blockLen);
            }
        }
        resultBuffer.writeInt(resultBuffer.getPosition(), 0);
    }

    @Override
    protected void serialize(HashMap<String, Object> dataMap, JsonMapper mapper) {

        var sections = new ArrayList<Map<String, Object>>();
        for (var item : this.sections) {
            sections.add(item.serialize(mapper));
        }
        dataMap.put("sections", sections);
    }

    @Override
    public void doDeserialize(JsonNode toDeserialize, JsonMapper mapper) {
        super.doDeserialize(toDeserialize, mapper);
        var sections = toDeserialize.get("sections");
        this.sections = new ArrayList<>();
        for (var i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            var om = new OpMsgSection();
            om.doDeserialize(section, mapper);
            this.sections.add(om);
        }
    }
}
