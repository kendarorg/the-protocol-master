package org.kendar.proxy;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;

import java.util.List;

public interface WireProxySocket {
    public void write(BBuffer buffer);
    public void write(ReturnMessage rm, BBuffer buffer);
    public List<ReturnMessage> read(ProtoState protoState, boolean optional);
    public void close();
    public boolean isConnected();
}
