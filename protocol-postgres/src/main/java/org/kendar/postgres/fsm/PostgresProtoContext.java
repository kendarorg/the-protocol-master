package org.kendar.postgres.fsm;

import org.kendar.iterators.IteratorOfLists;
import org.kendar.postgres.executor.PostgresExecutor;
import org.kendar.postgres.messages.ErrorResponse;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.ProxyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PostgresProtoContext extends NetworkProtoContext {

    private static final Logger log = LoggerFactory.getLogger(PostgresProtoContext.class);
    private static ConcurrentHashMap<Integer, PostgresProtoContext> pids = new ConcurrentHashMap<>();
    private final int pid;
    private final AtomicBoolean cancel = new AtomicBoolean(false);
    private List<Iterator<ProtoStep>> toSync = new ArrayList<>();
    private PostgresExecutor executor;

    public PostgresProtoContext(ProtoDescriptor descriptor, int contextId) {

        super(descriptor, contextId);
        pid = getNewPid();
        pids.put(pid, this);
    }

    public static void initializePids() {
        pids = new ConcurrentHashMap<>();
    }

    public static PostgresProtoContext getContextByPid(int pid) {
        return pids.get(pid);
    }

    @Override
    public void disconnect(Object connection) {

        super.disconnect(connection);
        var conn = getValue("CONNECTION");
        var c = ((Connection) ((ProxyConnection) conn).getConnection());
        try {
            if (c != null && !c.isValid(1)) {
                c.close();
            }
        } catch (Exception ex) {
            log.trace("Ignorable", ex);
        }
    }

    public int getPid() {
        return pid;
    }

    private int getNewPid() {
        return descriptor.getCounter("PID_COUNTER");
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

    @Override
    protected List<ReturnMessage> runException(Exception ex, ProtoState state, ProtocolEvent event) {

        var result = new ArrayList<>(super.runException(ex, state, event));
        log.error(ex.getMessage(), ex);
        result.add(new ErrorResponse(ex.getMessage()));
        return result;
    }

    public void cancel() {
        cancel.set(true);
    }

    public boolean shouldCancel() {
        var result = cancel.get();
        if (result) {
            cancel.set(false);
        }
        return result;
    }

    public void setExecutor(PostgresExecutor executor) {
        this.executor = executor;
    }

    public PostgresExecutor getExecutor() {
        return executor;
    }
}
