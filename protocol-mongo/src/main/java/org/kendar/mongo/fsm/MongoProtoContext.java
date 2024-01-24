package org.kendar.mongo.fsm;

import org.kendar.dtos.ProcessId;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoDescriptor;
import org.kendar.server.Channel;

import java.util.concurrent.atomic.AtomicInteger;

public class MongoProtoContext extends ProtoContext {
    private static final AtomicInteger processIdCounter = new AtomicInteger(1);
    private static final AtomicInteger reqResId = new AtomicInteger(1);

    public MongoProtoContext(ProtoDescriptor descriptor, Channel client) {
        super(descriptor, client);
        setValue("MONGO_PID", new ProcessId(getNewPid()));
    }

    public int getNewPid() {
        return processIdCounter.incrementAndGet();
    }

    public int getReqResId() {
        return reqResId.incrementAndGet();
    }
}
