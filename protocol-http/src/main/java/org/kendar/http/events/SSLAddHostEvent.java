package org.kendar.http.events;


import org.kendar.events.TpmEvent;

public class SSLAddHostEvent implements TpmEvent{
    private String host;

    public SSLAddHostEvent() {
    }

    public SSLAddHostEvent(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
