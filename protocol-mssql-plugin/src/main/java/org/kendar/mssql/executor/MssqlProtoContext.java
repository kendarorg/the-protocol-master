package org.kendar.mssql.executor;

import org.kendar.buffers.BBuffer;
import org.kendar.mssql.buffers.MssqlBBuffer;
import org.kendar.mssql.constants.DoneStatus;
import org.kendar.mssql.messages.DoneToken;
import org.kendar.mssql.messages.ErrorToken;
import org.kendar.mssql.messages.TdsReturnMessage;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.ProtocolEvent;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.kendar.proxy.ProxyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MssqlProtoContext extends NetworkProtoContext {

    public static final int DEFAULT_PACKET_SIZE = 4096;
    private static final Logger log = LoggerFactory.getLogger(MssqlProtoContext.class);
    private final AtomicBoolean cancel = new AtomicBoolean(false);
    private final AtomicInteger prepareHandleCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, String> preparedStatements = new ConcurrentHashMap<>();
    private final int spid;
    private int packetSize = DEFAULT_PACKET_SIZE;
    private MssqlExecutor executor;

    public MssqlProtoContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
        this.spid = (contextId % 0xFFFE) + 1;
    }

    @Override
    public BBuffer buildBuffer() {
        return new MssqlBBuffer();
    }

    @Override
    protected BBuffer buildBuffer(NetworkProtoDescriptor descriptor) {
        return new MssqlBBuffer();
    }

    @Override
    public void disconnect(Object connection) {

        super.disconnect(connection);
        var conn = getValue("CONNECTION");
        if (conn == null) return;
        var c = ((Connection) ((ProxyConnection) conn).getConnection());
        try {
            if (c != null && !c.isValid(1)) {
                c.close();
            }
        } catch (Exception ex) {
            log.trace("Ignorable", ex);
        }
    }

    @Override
    protected List<ReturnMessage> runException(Exception ex, ProtoState state, ProtocolEvent event) {

        var result = new ArrayList<>(super.runException(ex, state, event));
        log.error(ex.getMessage(), ex);
        result.add(newMessage()
                .add(new ErrorToken(ex.getMessage()))
                .add(new DoneToken(DoneStatus.DONE_ERROR, 0)));
        return result;
    }

    public TdsReturnMessage newMessage() {
        return new TdsReturnMessage(packetSize, spid);
    }

    public int getSpid() {
        return spid;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public int storePreparedStatement(String query) {
        var handle = prepareHandleCounter.getAndIncrement();
        preparedStatements.put(handle, query);
        return handle;
    }

    public String getPreparedStatement(int handle) {
        return preparedStatements.get(handle);
    }

    public void removePreparedStatement(int handle) {
        preparedStatements.remove(handle);
    }

    public void cancel() {
        cancel.set(true);
        var executing = getValue("EXECUTING_NOW");
        if (executing != null) {
            try {
                ((Statement) executing).cancel();
            } catch (Exception ex) {
                log.trace("Ignorable", ex);
            }
        }
    }

    public boolean shouldCancel() {
        var result = cancel.get();
        if (result) {
            cancel.set(false);
        }
        return result;
    }

    public MssqlExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(MssqlExecutor executor) {
        this.executor = executor;
    }
}
