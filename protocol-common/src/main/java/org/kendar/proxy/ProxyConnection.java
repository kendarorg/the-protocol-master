package org.kendar.proxy;

public class ProxyConnection {
    private final Object connection;

    public ProxyConnection(Object connection) {

        this.connection = connection;
    }

    public Object getConnection() {
        return connection;
    }
}
