package org.kendar.apis.dtos;

public class ProtocolIndex {
    private final String id;
    private final String protocol;

    public ProtocolIndex(String id, String protocol) {

        this.id = id;
        this.protocol = protocol;
    }

    public String getId() {
        return id;
    }

    public String getProtocol() {
        return protocol;
    }
}
