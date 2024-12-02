package org.kendar.mongo;

import org.junit.jupiter.api.Test;
import org.kendar.mongo.dtos.MongoCommandsConstants;

public class ActionIdSection {
    @Test
    void test() {
        var data="{\"flags\":0,\"opCode\":\"OP_MSG\",\"sections\":[{\"documents\":[{\"ok\":true,\"ismaster\":true,\"maxBsonObjectSize\":{\"$numberInt\":\"16777216\"},\"maxMessageSizeBytes\":{\"$numberInt\":\"48000000\"},\"maxWriteBatchSize\":{\"$numberInt\":\"100000\"},\"localTime\":{\"$date\":{\"$numberLong\":\"1709545404773\"}},\"logicalSessionTimeoutMinutes\":{\"$numberInt\":\"30\"},\"connectionId\":{\"$numberInt\":\"1\"},\"minWireVersion\":{\"$numberInt\":\"0\"},\"maxWireVersion\":{\"$numberInt\":\"8\"},\"readOnly\":false}]}]}";

        var val = MongoCommandsConstants.insert;
        System.out.println(val);
        var result = MongoCommandsConstants.valueOf("insert");
        System.out.println(result);
       // var doc = new Document();
    }
}
