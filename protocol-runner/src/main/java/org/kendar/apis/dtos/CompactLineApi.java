package org.kendar.apis.dtos;

import org.kendar.storage.CompactLineComplete;

public class CompactLineApi extends CompactLineComplete {
    private String fullItemAddress;

    public void setFullItemAddress(String fullItemAddress) {
        this.fullItemAddress = fullItemAddress;
    }

    public String getFullItemAddress() {
        return fullItemAddress;
    }
}
