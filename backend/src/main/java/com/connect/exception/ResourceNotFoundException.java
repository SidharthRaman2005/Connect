package com.connect.exception;

/**
 * Thrown when a requested resource (e.g. a Student) does not exist.
 * Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
