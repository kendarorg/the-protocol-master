package org.kendar.ui.dto;

import org.kendar.storage.CompactLineComplete;
import org.kendar.storage.StorageItem;

public class RecordingLine {
    private StorageItem data;
    private CompactLineComplete index;

    public RecordingLine() {
    }

    public RecordingLine(CompactLineComplete index) {
        this.index = index;
    }

    public CompactLineComplete getIndex() {
        return index;
    }

    public void setIndex(CompactLineComplete index) {
        this.index = index;
    }

    public StorageItem getData() {
        return data;
    }

    public void setData(StorageItem data) {
        this.data = data;
    }
}
