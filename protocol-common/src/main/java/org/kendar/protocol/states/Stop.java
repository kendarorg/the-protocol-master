package org.kendar.protocol.states;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.messages.ReturnMessage;

/**
 * Special stop state (internal)
 */
public class Stop implements ProtoStep, ReturnMessage, NetworkReturnMessage {

    @Override
    public ReturnMessage run() {
        return null;
    }

    @Override
    public void write(BBuffer resultBuffer) {

    }
}
