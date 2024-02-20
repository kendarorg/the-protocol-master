package org.kendar.mysql.executor;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.ErrorCode;
import org.kendar.mysql.messages.Error;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.descriptor.NetworkProtoDescriptor;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BaseEvent;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MySQLProtoContext extends NetworkProtoContext {
    private static final AtomicInteger processIdCounter = new AtomicInteger(0);
    private int clientCapabilities;
    private int packetNumber = -1;

    public MySQLProtoContext(ProtoDescriptor descriptor) {
        super(descriptor);
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
        return processIdCounter.incrementAndGet();
    }

    @Override
    protected BBuffer buildBuffer(NetworkProtoDescriptor descriptor) {
        return new MySQLBBuffer(descriptor.isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }

    private static Logger log = LoggerFactory.getLogger(MySQLProtoContext.class);

    protected List<ReturnMessage> runExceptionInternal(Exception ex, ProtoState state, BaseEvent event) {
        var result = new ArrayList<ReturnMessage>(super.runExceptionInternal(ex, state, event));
        log.error(ex.getMessage(),ex);
        var error = new Error();
        error.setCapabilities(this.getClientCapabilities());
        error.setErrorCode(ErrorCode.ER_UNKNOWN_COM_ERROR.getValue());
        error.setErrorMessage(ex.getMessage());
        error.setSqlState("08S01");
        result.add(error);
        return result;
    }
}
