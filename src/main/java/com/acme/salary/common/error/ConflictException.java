package com.acme.salary.common.error;

/** Thrown when the request conflicts with the current state of a resource; mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
