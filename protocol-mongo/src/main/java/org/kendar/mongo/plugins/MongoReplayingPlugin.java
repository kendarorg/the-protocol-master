package org.kendar.mongo.plugins;

import org.kendar.filters.BasicReplayingPlugin;
import org.kendar.mongo.dtos.OpMsgContent;
import org.kendar.mongo.dtos.OpReplyContent;
import org.kendar.mongo.fsm.MongoProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.FilterContext;
import org.kendar.storage.StorageItem;

import java.util.List;

public class MongoReplayingPlugin extends BasicReplayingPlugin {
    @Override
    public String getProtocol() {
        return "mongodb";
    }


    protected void buildState(FilterContext filterContext, ProtoContext context,
                              Object in, Object outputData, Object out) {
        switch (filterContext.getType()) {
            case ("OP_MSG"):
            case ("HELLO_OP_MSG"):
                var data = (OpMsgContent) in;
                var res = (OpMsgContent) out;
                res.doDeserialize(mapper.toJsonNode(outputData), mapper);
                res.setRequestId(((MongoProtoContext) filterContext.getContext()).getReqResId());
                res.setResponseId(data.getRequestId());
                res.setFlags(8);
                break;
            case ("HELLO_OP_QUERY"):
                var data1 = (OpMsgContent) in;
                var res1 = (OpReplyContent) out;
                res1.doDeserialize(mapper.toJsonNode(outputData), mapper);
                res1.setRequestId(((MongoProtoContext) filterContext.getContext()).getReqResId());
                res1.setResponseId(data1.getRequestId());
                res1.setFlags(8);
                break;
        }
    }

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> result) {
        throw new RuntimeException("Not async");
    }


}
