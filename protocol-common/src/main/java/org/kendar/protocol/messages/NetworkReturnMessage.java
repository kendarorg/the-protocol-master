package org.kendar.protocol.messages;

import org.kendar.buffers.BBuffer;

/**
 * Default interface for messages returned by proto step that should be sent
 * through network
 */
public interface NetworkReturnMessage extends ReturnMessage {
    void write(BBuffer resultBuffer);
}
