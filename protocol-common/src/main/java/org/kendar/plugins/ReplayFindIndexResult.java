package org.kendar.plugins;

import org.kendar.storage.CompactLine;

public class ReplayFindIndexResult {
    private CompactLine line;
    private boolean repeated;

    public ReplayFindIndexResult(CompactLine line, boolean repeated) {
        this.line = line;
        this.repeated = repeated;
    }

    public CompactLine getLine() {
        return line;
    }

    public void setLine(CompactLine line) {
        this.line = line;
    }

    public boolean isRepeated() {
        return repeated;
    }

    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }
}
