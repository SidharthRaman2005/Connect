package com.connect.security;

/**
 * The authentication principal placed in the security context for a request
 * carrying a valid session JWT. Controllers can access it with
 * {@code @AuthenticationPrincipal AuthenticatedStudent principal}.
 *
 * @param id    the authenticated student's id (JWT subject)
 * @param email the student's email (JWT claim)
 */
public record AuthenticatedStudent(Long id, String email) {
}
