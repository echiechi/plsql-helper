package com.plsql.tools.exceptions;

public class PlsqlException extends RuntimeException {
    public PlsqlException() {
        super();
    }

    public PlsqlException(String message) {
        super(message);
    }

    public PlsqlException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlsqlException(Throwable cause) {
        super(cause);
    }

    protected PlsqlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
