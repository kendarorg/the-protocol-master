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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
