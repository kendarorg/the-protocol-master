package org.kendar.apis.dtos;

public class ProtocolIndex {
    private  String id;
    private  String protocol;
    public ProtocolIndex(){

    }
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
