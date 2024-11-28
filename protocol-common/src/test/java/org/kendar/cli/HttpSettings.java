package org.kendar.cli;

public class HttpSettings implements ProtocolSetting {
    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
