package org.kendar.exceptions;

public class UnknownCommandException extends RuntimeException {

    public UnknownCommandException(String message) {

        super(message);
    }

    public UnknownCommandException(String message, Throwable exception) {

        super(message,exception);
    }

}
