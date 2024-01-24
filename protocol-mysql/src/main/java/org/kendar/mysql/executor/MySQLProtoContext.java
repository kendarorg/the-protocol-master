package org.kendar.mysql.executor;

import org.kendar.buffers.BBuffer;
import org.kendar.buffers.BBufferEndianness;
import org.kendar.mysql.buffers.MySQLBBuffer;
import org.kendar.mysql.constants.ErrorCode;
import org.kendar.mysql.messages.Error;
import org.kendar.protocol.ProtoContext;
import org.kendar.protocol.ProtoDescriptor;
import org.kendar.protocol.ReturnMessage;
import org.kendar.server.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MySQLProtoContext extends ProtoContext {
    private static final AtomicInteger processIdCounter = new AtomicInteger(0);
    private int clientCapabilities;
    private int packetNumber = -1;

    public MySQLProtoContext(ProtoDescriptor descriptor, Channel client) {
        super(descriptor, client);
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
    protected BBuffer buildBuffer(ProtoDescriptor descriptor) {
        return new MySQLBBuffer(descriptor.isBe() ? BBufferEndianness.BE : BBufferEndianness.LE);
    }

    protected List<ReturnMessage> runExceptionInternal(Exception ex) {
        var result = new ArrayList<ReturnMessage>();
        ex.printStackTrace();
        var error = new Error();
        error.setCapabilities(this.getClientCapabilities());
        error.setErrorCode(ErrorCode.ER_UNKNOWN_COM_ERROR.getValue());
        error.setErrorMessage(ex.getMessage());
        error.setSqlState("08S01");
        result.add(error);
        return result;
    }
}
