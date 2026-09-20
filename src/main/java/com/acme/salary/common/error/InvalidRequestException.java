package com.acme.salary.common.error;

/** Thrown when a request is well-formed but violates a business rule; mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
