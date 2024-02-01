package org.kendar.protocol;

import org.kendar.buffers.BBuffer;

public interface ReturnMessage {
    void write(BBuffer resultBuffer);
}
