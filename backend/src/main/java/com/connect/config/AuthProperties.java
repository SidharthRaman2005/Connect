package com.connect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication configuration bound from the {@code app.auth.*} properties.
 *
 * @param googleClientId       the Google OAuth client ID accepted as the
 *                             ID-token audience; must match the frontend's
 * @param allowedEmailDomain   the only email domain permitted to sign in
 * @param jwtSecret            HMAC signing key for session JWTs; if blank, an
 *                             ephemeral key is generated at startup (dev only)
 * @param jwtExpirationSeconds session JWT lifetime in seconds
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String googleClientId,
        String allowedEmailDomain,
        String jwtSecret,
        long jwtExpirationSeconds) {
}
