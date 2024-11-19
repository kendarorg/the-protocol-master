package org.kendar.events;

import org.kendar.storage.generic.LineToWrite;

public class WriteItemEvent implements TpmEvent {
    private final LineToWrite lineToWrite;

    public WriteItemEvent(LineToWrite lineToWrite) {
        this.lineToWrite = lineToWrite;
    }

    public LineToWrite getLineToWrite() {
        return lineToWrite;
    }
}
