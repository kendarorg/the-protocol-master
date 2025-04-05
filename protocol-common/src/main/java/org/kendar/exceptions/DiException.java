package org.kendar.exceptions;

public class DiException extends TPMException {

    public DiException(String message) {
        super(message);
    }

    public DiException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiException(Throwable cause) {
        super(cause);
    }

    protected DiException(String message, Throwable cause,
                          boolean enableSuppression,
                          boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
