package com.connect.dto;

/**
 * Response for a successful sign-in.
 *
 * @param token      the session JWT the client sends on subsequent requests
 * @param newAccount true if the account was created by this login (route to
 *                   onboarding); false for a returning student
 * @param student    the authenticated student's public profile
 */
public record AuthResponse(String token, boolean newAccount, StudentDto student) {
}
