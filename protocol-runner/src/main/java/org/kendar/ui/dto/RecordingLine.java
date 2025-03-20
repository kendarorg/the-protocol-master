package org.kendar.ui.dto;

import org.kendar.storage.CompactLineComplete;
import org.kendar.storage.StorageItem;

public class RecordingLine {
    private StorageItem data;

    public RecordingLine() {
    }

    public CompactLineComplete getIndex() {
        return index;
    }

    public void setIndex(CompactLineComplete index) {
        this.index = index;
    }

    private CompactLineComplete index;

    public RecordingLine(CompactLineComplete index) {
        this.index = index;
    }

    public void setData(StorageItem data) {
        this.data = data;
    }

    public StorageItem getData() {
        return data;
    }
}
