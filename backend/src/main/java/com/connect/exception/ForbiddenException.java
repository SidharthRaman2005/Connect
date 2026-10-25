package com.connect.exception;

/**
 * Thrown when an authenticated student is not permitted to access a specific
 * resource (e.g. a private profile). Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
