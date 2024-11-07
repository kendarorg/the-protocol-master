package org.kendar.settings;

public class ByteProtocolSettings extends ProtocolSettings{
    private int port;
    private String connectionString;
    private int timeoutSeconds;
    private DefaultSimulationSettings simulation;

    public DefaultSimulationSettings getSimulation() {
        return simulation;
    }

    public void setSimulation(DefaultSimulationSettings simulation) {
        this.simulation = simulation;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }
}