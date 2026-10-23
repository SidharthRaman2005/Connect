package com.connect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication configuration bound from the {@code app.auth.*} properties.
 *
 * @param googleClientId    the Google OAuth client ID accepted as the ID-token
 *                          audience; must match the frontend's client ID
 * @param allowedEmailDomain the only email domain permitted to sign in
 *                           (e.g. {@code vitapstudent.ac.in})
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(String googleClientId, String allowedEmailDomain) {
}
