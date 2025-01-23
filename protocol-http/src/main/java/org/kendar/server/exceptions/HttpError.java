package org.kendar.server.exceptions;

import java.io.Serial;

public class HttpError extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 8769596371344178179L;

    public HttpError(String msg) {
        super(msg);
    }
}
