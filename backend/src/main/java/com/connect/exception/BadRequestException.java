package com.connect.exception;

/**
 * Thrown when a request is well-formed but semantically invalid — e.g. it
 * references a tag that does not exist or is not approved. Maps to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
