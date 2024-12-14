package org.kendar.storage;

public class CompactLineComplete extends CompactLine {
    private String protocolInstanceId;
    private String fullItemId;

    public void setProtocolInstanceId(String protocol) {
        this.protocolInstanceId = protocol;
    }

    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }

    public void setFullItemId(String fullItemId) {
        this.fullItemId = fullItemId;
    }

    public String getFullItemId() {
        return fullItemId;
    }
}
