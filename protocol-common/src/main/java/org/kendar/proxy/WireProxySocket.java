package org.kendar.proxy;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.ReturnMessage;
import org.kendar.protocol.states.ProtoState;

import java.util.List;

public interface WireProxySocket {
    void write(BBuffer buffer);

    void write(ReturnMessage rm, BBuffer buffer);

    List<ReturnMessage> read(ProtoState protoState, boolean optional);

    void close();

    boolean isConnected();
}
