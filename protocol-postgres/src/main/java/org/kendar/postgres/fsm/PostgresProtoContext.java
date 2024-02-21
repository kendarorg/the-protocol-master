package org.kendar.postgres.fsm;

import org.kendar.iterators.IteratorOfLists;
import org.kendar.postgres.messages.ErrorResponse;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PostgresProtoContext extends NetworkProtoContext {

    private static final AtomicInteger processIdCounter = new AtomicInteger(0);
    private static final ConcurrentHashMap<Integer, PostgresProtoContext> pids = new ConcurrentHashMap<>();
    private final int pid;
    private List<Iterator<ProtoStep>> toSync = new ArrayList<>();
    private final AtomicBoolean cancel = new AtomicBoolean(false);

    public PostgresProtoContext(ProtoDescriptor descriptor) {

        super(descriptor);
        pid = getNewPid();
        pids.put(pid, this);
    }

    public static PostgresProtoContext getContextByPid(int pid) {
        return pids.get(pid);
    }

    public int getPid() {
        return pid;
    }

    private int getNewPid() {
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

    public void stop() {
        run.set(false);
        inputQueue.clear();
    }
    private static Logger log = LoggerFactory.getLogger(PostgresProtoContext.class);
    @Override
    protected List<ReturnMessage> runExceptionInternal(Exception ex, ProtoState state, BaseEvent event) {

        var result = new ArrayList<ReturnMessage>(super.runExceptionInternal(ex, state, event));
        log.error(ex.getMessage(),ex);
        result.add(new ErrorResponse(ex.getMessage()));
        return result;
    }

    public void cancel() {
        cancel.set(true);
        inputQueue.clear();
    }

    public boolean shouldCancel() {
        var result = cancel.get();
        if (result) {
            cancel.set(false);
        }
        return result;
    }
}
