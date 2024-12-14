package org.kendar.apis.dtos;

import org.kendar.storage.CompactLineComplete;

public class CompactLineApi extends CompactLineComplete {
    private String fullItemAddress;

    public String getFullItemAddress() {
        return fullItemAddress;
    }

    public void setFullItemAddress(String fullItemAddress) {
        this.fullItemAddress = fullItemAddress;
    }
}
