package org.kendar.postgres.fsm;

import org.kendar.postgres.messages.ErrorResponse;
import org.kendar.protocol.*;
import org.kendar.server.Channel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PostgresProtoContext extends ProtoContext {
    private static final AtomicInteger processIdCounter = new AtomicInteger(0);
    private List<Iterator<ProtoStep>> toSync = new ArrayList<>();

    public PostgresProtoContext(ProtoDescriptor descriptor, Channel client) {
        super(descriptor, client);
    }

    public int getNewPid() {
        return processIdCounter.incrementAndGet();
    }

    public void addSync(Iterator<ProtoStep> message) {
        toSync.add(message);
    }


    public Iterator<ProtoStep> clearSync() {
        var res = toSync;
        toSync = new ArrayList<>();
        var result = new IteratorOfLists<ProtoStep>();
        for (var it : res) {
            result.addIterator(it);
        }
        return result;
    }

    protected List<ReturnMessage> runExceptionInternal(Exception ex) {
        var result = new ArrayList<ReturnMessage>();
        ex.printStackTrace();
        result.add(new ErrorResponse(ex.getMessage()));
        return result;
    }
}
