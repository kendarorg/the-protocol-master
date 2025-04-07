package org.kendar.exceptions;

public class TPMProtocolException extends TPMException {

    public TPMProtocolException(String message) {
        super(message);
    }

    public TPMProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public TPMProtocolException(Throwable cause) {
        super(cause);
    }

    protected TPMProtocolException(String message, Throwable cause,
                                   boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
