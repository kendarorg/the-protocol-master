package org.kendar.storage;

public class StorageFile {
    private StorageFileIndex index;
    private String content;

    public StorageFile() {
    }

    public StorageFile(StorageFileIndex index, String content) {
        this.index = index;
        this.content = content;
    }

    public StorageFileIndex getIndex() {
        return index;
    }

    public void setIndex(StorageFileIndex index) {
        this.index = index;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
