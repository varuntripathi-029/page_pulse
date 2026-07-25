package com.digitalheroes.pagepulse.exception;

public class UrlFetchException extends RuntimeException {

    public UrlFetchException(String message) {
        super(message);
    }

    public UrlFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
