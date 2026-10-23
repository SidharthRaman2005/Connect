package com.connect.exception;

/**
 * Thrown when a token is otherwise valid but the verified email does not belong
 * to the allowed college domain. Maps to HTTP 403 Forbidden.
 */
public class EmailDomainNotAllowedException extends RuntimeException {

    public EmailDomainNotAllowedException(String message) {
        super(message);
    }
}
