package com.connect.exception;

import java.time.Instant;

/**
 * Standard JSON error body returned by the API.
 *
 * @param status    HTTP status code
 * @param error     short reason phrase
 * @param message   human-readable detail safe to show the user
 * @param timestamp when the error was produced
 */
public record ApiError(int status, String error, String message, Instant timestamp) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now());
    }
}
