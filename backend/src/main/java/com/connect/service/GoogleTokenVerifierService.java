package com.connect.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.connect.config.AuthProperties;
import com.connect.dto.VerifiedUser;
import com.connect.exception.EmailDomainNotAllowedException;
import com.connect.exception.InvalidTokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Verifies Google ID tokens and enforces the college email restriction.
 *
 * <p>{@link GoogleIdTokenVerifier#verify} checks the token's signature against
 * Google's public keys and validates the issuer (accounts.google.com), the
 * audience (our client ID) and the expiry. On top of that we require the email
 * to be present and verified, and to belong to the allowed college domain.
 */
@Service
public class GoogleTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);

    private final GoogleIdTokenVerifier verifier;
    private final String allowedDomain;

    public GoogleTokenVerifierService(AuthProperties authProperties) {
        this.allowedDomain = authProperties.allowedEmailDomain().toLowerCase(Locale.ROOT);
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(authProperties.googleClientId()))
                .build();
    }

    /**
     * Fully verifies a Google ID token string and returns the college identity.
     *
     * @throws InvalidTokenException        if the token is malformed, has a bad
     *                                      signature/issuer/audience, is expired,
     *                                      or the email is missing/unverified
     * @throws EmailDomainNotAllowedException if the email is not in the allowed domain
     */
    public VerifiedUser verify(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            throw new InvalidTokenException("Could not verify the Google token.");
        } catch (IllegalArgumentException e) {
            // The library throws this when the string isn't a well-formed JWT.
            log.warn("Malformed Google token: {}", e.getMessage());
            throw new InvalidTokenException("Malformed Google ID token.");
        }
        if (idToken == null) {
            // Signature, issuer, audience or expiry check failed.
            throw new InvalidTokenException("Invalid or expired Google ID token.");
        }
        return validateClaims(idToken.getPayload());
    }

    /**
     * Enforces the verified-email and college-domain rules on an already
     * signature-verified payload, and maps it to a {@link VerifiedUser}.
     * Package-private so it can be unit-tested without a real token.
     */
    VerifiedUser validateClaims(Payload payload) {
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();

        if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
            throw new InvalidTokenException("Email is missing or not verified by Google.");
        }

        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        int atIndex = normalizedEmail.lastIndexOf('@');
        String domain = atIndex >= 0 ? normalizedEmail.substring(atIndex + 1) : "";
        if (!domain.equals(allowedDomain)) {
            throw new EmailDomainNotAllowedException(
                    "Only @" + allowedDomain + " accounts are allowed to sign in.");
        }

        return new VerifiedUser(
                payload.getSubject(),
                normalizedEmail,
                (String) payload.get("name"),
                (String) payload.get("picture"));
    }
}
