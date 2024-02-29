package org.kendar.proxy;

/**
 * Connection wrapper. Can be ANY KIND OF THING. An sql connection, a socket
 * a file...wetheaver. It is stored on the network context
 */
public class ProxyConnection {
    private final Object connection;

    public ProxyConnection(Object connection) {
        this.connection = connection;
    }

    public Object getConnection() {
        return connection;
    }
}
