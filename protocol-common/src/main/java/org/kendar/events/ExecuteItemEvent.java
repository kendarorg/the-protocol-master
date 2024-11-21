package org.kendar.events;

import org.kendar.storage.generic.LineToWrite;

public class ExecuteItemEvent implements TpmEvent {
    private final LineToWrite lineToWrite;

    public ExecuteItemEvent(LineToWrite lineToWrite) {
        this.lineToWrite = lineToWrite;
    }

    public LineToWrite getLineToWrite() {
        return lineToWrite;
    }
}
