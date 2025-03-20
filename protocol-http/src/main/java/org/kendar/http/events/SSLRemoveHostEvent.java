package org.kendar.http.events;

import org.kendar.events.TpmEvent;

public class SSLRemoveHostEvent implements TpmEvent {
    String host;

    public SSLRemoveHostEvent() {

    }

    public SSLRemoveHostEvent(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
