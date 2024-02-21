package org.kendar.protocol.messages;

import org.kendar.buffers.BBuffer;

public interface NetworkReturnMessage extends ReturnMessage {
    void write(BBuffer resultBuffer);
}
