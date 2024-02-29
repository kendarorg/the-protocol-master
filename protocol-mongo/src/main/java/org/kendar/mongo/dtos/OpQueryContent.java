package org.kendar.mongo.dtos;

import com.fasterxml.jackson.databind.JsonNode;
import org.kendar.buffers.BBuffer;
import org.kendar.mongo.fsm.OpCodes;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;

public class OpQueryContent extends BaseMessageData {


    private final String fullCollectionName;
    private final int numberToSkip;
    private final int numberToReturn;
    private final ArrayList<String> documents;

    public OpQueryContent(int flags, int requestId, int responseId, String fullCollectionName,
                          int numberToSkip, int numberToReturn, ArrayList<String> documents) {
        super(flags, requestId, responseId, OpCodes.OP_QUERY);
        this.fullCollectionName = fullCollectionName;
        this.numberToSkip = numberToSkip;
        this.numberToReturn = numberToReturn;
        this.documents = documents;
    }

    public String getFullCollectionName() {
        return fullCollectionName;
    }

    public int getNumberToSkip() {
        return numberToSkip;
    }

    public int getNumberToReturn() {
        return numberToReturn;
    }

    public ArrayList<String> getDocuments() {
        return documents;
    }

    @Override
    public void write(BBuffer resultBuffer) {
        super.write(resultBuffer);
    }

    @Override
    protected void serialize(HashMap<String, Object> dataMap, JsonMapper mapper) {
        dataMap.put("fullCollectionName", fullCollectionName);
        dataMap.put("numberToSkip", numberToSkip);
        dataMap.put("numberToReturn", numberToReturn);
        var list = new ArrayList<JsonNode>();
        for (var item : documents) {
            list.add(mapper.toJsonNode(item));
        }
        dataMap.put("documents", list);
    }
}
