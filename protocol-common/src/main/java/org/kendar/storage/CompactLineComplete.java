package org.kendar.storage;

public class CompactLineComplete extends CompactLine {
    private String protocolInstanceId;
    private String fullItemId;

    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }

    public void setProtocolInstanceId(String protocol) {
        this.protocolInstanceId = protocol;
    }

    public String getFullItemId() {
        return fullItemId;
    }

    public void setFullItemId(String fullItemId) {
        this.fullItemId = fullItemId;
    }
}
