package org.kendar.mongo.fsm;

import org.kendar.iterators.ProcessId;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;

import java.util.concurrent.atomic.AtomicInteger;

public class MongoProtoContext extends NetworkProtoContext {
    private static final AtomicInteger processIdCounter = new AtomicInteger(1);
    private static final AtomicInteger reqResId = new AtomicInteger(1);

    public MongoProtoContext(ProtoDescriptor descriptor) {
        super(descriptor);
        setValue("MONGO_PID", new ProcessId(getNewPid()));
    }

    public int getNewPid() {
        return processIdCounter.incrementAndGet();
    }

    public int getReqResId() {
        return reqResId.incrementAndGet();
    }
}
