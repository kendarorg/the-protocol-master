package org.kendar.mongo.fsm;

import org.kendar.iterators.ProcessId;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;

public class MongoProtoContext extends NetworkProtoContext {

    public MongoProtoContext(ProtoDescriptor descriptor) {
        super(descriptor);
        setValue("MONGO_PID", new ProcessId(getNewPid()));
    }

    public int getNewPid() {
        return ProtoDescriptor.getCounter("PID_COUNTER");
    }

    public int getReqResId() {
        return ProtoDescriptor.getCounter("REQ_ID_COUNTER");
    }
}
