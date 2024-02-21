package org.kendar.exceptions;

public class ConnectionExeception extends RuntimeException {

    public ConnectionExeception(String message) {

        super(message);
    }

    public ConnectionExeception(String message, Throwable exception) {

        super(message,exception);
    }

}
