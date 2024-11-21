package org.kendar.events;

import org.kendar.storage.generic.LineToWrite;

public class ReplayItemEvent implements TpmEvent {
    private final LineToWrite lineToWrite;

    public ReplayItemEvent(LineToWrite lineToWrite) {
        this.lineToWrite = lineToWrite;
    }

    public LineToWrite getLineToWrite() {
        return lineToWrite;
    }
}
