package org.kendar.protocol;

import org.kendar.buffers.BBuffer;

public abstract class ReturnMessage {
    public abstract void write(BBuffer resultBuffer);
}
