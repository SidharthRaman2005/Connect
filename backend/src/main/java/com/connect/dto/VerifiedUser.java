package com.connect.dto;

/**
 * The identity extracted from a Google ID token after it has been fully
 * verified (signature, issuer, audience, expiry, verified college email).
 *
 * <p>In M1.1 this is returned directly to confirm validation works. Later
 * modules use it to create/authenticate a {@code Student} and issue a session.
 *
 * @param googleId the Google account subject ({@code sub}) — a stable unique id
 * @param email    the verified college email address
 * @param name     the user's display name (may be null)
 * @param picture  URL of the user's profile picture (may be null)
 */
public record VerifiedUser(String googleId, String email, String name, String picture) {
}
