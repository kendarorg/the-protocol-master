package org.kendar.cli;

public class PostgresSettings implements ProtocolSetting {
    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
