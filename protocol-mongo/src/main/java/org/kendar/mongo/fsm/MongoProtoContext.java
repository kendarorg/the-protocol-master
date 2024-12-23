package org.kendar.mongo.fsm;

import com.mongodb.client.MongoClient;
import org.kendar.iterators.ProcessId;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.proxy.ProxyConnection;

public class MongoProtoContext extends NetworkProtoContext {

    public MongoProtoContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
        setValue("MONGO_PID", new ProcessId(getNewPid()));
    }

    public int getNewPid() {
        return descriptor.getCounter("PID_COUNTER");
    }

    public int getReqResId() {
        return descriptor.getCounter("REQ_ID_COUNTER");
    }

    @Override
    public void disconnect(Object connection) {

        super.disconnect(connection);
        var mongoClient = ((MongoClient) ((ProxyConnection) getValue("CONNECTION")).getConnection());
        if (mongoClient != null) mongoClient.close();
    }
}
