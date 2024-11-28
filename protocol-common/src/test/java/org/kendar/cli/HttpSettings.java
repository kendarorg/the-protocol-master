package org.kendar.cli;

public class HttpSettings implements ProtocolSetting {
    private int port;
    private boolean active;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
