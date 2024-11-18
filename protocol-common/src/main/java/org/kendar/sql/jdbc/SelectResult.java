package org.kendar.sql.jdbc;

import java.util.ArrayList;
import java.util.List;

public class SelectResult {
    private final List<List<String>> records = new ArrayList<>();
    private final List<ProxyMetadata> metadata = new ArrayList<>();
    private int count;


    public SelectResult copy(){
        var cloned = new SelectResult();
        cloned.setCount(this.count);
        for(var record : records){
            cloned.records.add(new ArrayList<>(record));
        }
        for(var metadataItem : metadata){
            cloned.metadata.add(metadataItem.copy());
        }
        cloned.intResult = this.intResult;
        cloned.lastInsertedId = this.lastInsertedId;
        return cloned;
    }
    private boolean intResult;
    private long lastInsertedId;

    public List<List<String>> getRecords() {
        return records;
    }

    public List<ProxyMetadata> getMetadata() {
        return metadata;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isIntResult() {
        return intResult;
    }

    public void setIntResult(boolean intResult) {
        this.intResult = intResult;
    }

    public long getLastInsertedId() {
        return lastInsertedId;
    }

    public void setLastInsertedId(long lastInsertedId) {
        this.lastInsertedId = lastInsertedId;
    }

    public void fill(SelectResult source) {
        this.intResult = source.intResult;
        this.lastInsertedId = source.lastInsertedId;
        this.count = source.count;
        this.metadata.addAll(source.metadata);
        this.records.addAll(source.records);

    }
}
