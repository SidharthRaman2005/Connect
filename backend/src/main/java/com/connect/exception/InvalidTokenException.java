package com.connect.exception;

/**
 * Thrown when a Google ID token cannot be verified: bad signature, wrong
 * issuer/audience, expired, or the email is missing/unverified.
 * Maps to HTTP 401 Unauthorized.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
