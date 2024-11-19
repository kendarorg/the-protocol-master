package org.kendar.plugins;

import org.kendar.storage.StorageItem;

public class MockStorage extends StorageItem {
    private int nthRequest;
    private int count;

    public int getNthRequest() {
        return nthRequest;
    }

    public void setNthRequest(int nthRequest) {
        this.nthRequest = nthRequest;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
