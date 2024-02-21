package org.kendar.exceptions;

public class UnknownHandlerException extends RuntimeException {

    public UnknownHandlerException(String message) {

        super(message);
    }

    public UnknownHandlerException(String message, Throwable exception) {

        super(message,exception);
    }

}
