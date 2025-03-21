package org.kendar.exceptions;

public class TPMException extends RuntimeException {
    public TPMException(String message) {
        super(message);
    }
    public TPMException(String message, Throwable cause) {
        super(message, cause);
    }
    public TPMException(Throwable cause) {
        super(cause);
    }
    protected TPMException(String message, Throwable cause,
                               boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
