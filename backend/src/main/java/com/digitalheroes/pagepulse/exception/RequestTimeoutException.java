package com.digitalheroes.pagepulse.exception;

public class RequestTimeoutException extends RuntimeException {

    public RequestTimeoutException(String message) {
        super(message);
    }
}
