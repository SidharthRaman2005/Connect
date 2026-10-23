package com.connect.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/google}.
 *
 * @param idToken the Google ID token (JWT credential) returned by Google
 *                Sign-In on the frontend
 */
public record GoogleLoginRequest(@NotBlank(message = "idToken is required") String idToken) {
}
