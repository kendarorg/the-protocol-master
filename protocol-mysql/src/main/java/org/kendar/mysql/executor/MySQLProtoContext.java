package org.kendar.mysql.executor;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.ErrorCode;
import org.kendar.mysql.messages.Error;
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
import java.util.ArrayList;
import java.util.List;

public class MySQLProtoContext extends NetworkProtoContext {
    private static final Logger log = LoggerFactory.getLogger(MySQLProtoContext.class);
    private int clientCapabilities;
    private int packetNumber = -1;
    private MySQLExecutor executor;

    public MySQLProtoContext(ProtoDescriptor descriptor, int contextId) {
        super(descriptor, contextId);
    }

    public int getClientCapabilities() {
        return clientCapabilities;
    }

    public void setClientCapabilities(int clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
    }

    public int getPacketNumber() {
        packetNumber++;
        return packetNumber;
    }

    public void setPacketNumber(int packetNumber) {
        this.packetNumber = packetNumber;
    }

    public int getNewPid() {
        return descriptor.getCounter("PID_COUNTER");
    }

    @Override
    protected BBuffer buildBuffer(NetworkProtoDescriptor descriptor) {
        return new MySQLBBuffer(descriptor.isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
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

    protected List<ReturnMessage> runException(Exception ex, ProtoState state, ProtocolEvent event) {
        var result = new ArrayList<>(super.runException(ex, state, event));
        log.error(ex.getMessage(), ex);
        var error = new Error();
        error.setCapabilities(this.getClientCapabilities());
        error.setErrorCode(ErrorCode.ER_UNKNOWN_COM_ERROR.getValue());
        error.setErrorMessage(ex.getMessage());
        error.setSqlState("08S01");
        result.add(error);
        return result;
    }

    public MySQLExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(MySQLExecutor executor) {
        this.executor = executor;
    }
}
