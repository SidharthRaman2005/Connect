package com.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.connect.config.AuthProperties;
import com.connect.dto.VerifiedUser;
import com.connect.exception.EmailDomainNotAllowedException;
import com.connect.exception.InvalidTokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

/**
 * Unit tests for the claim/domain enforcement in {@link GoogleTokenVerifierService}.
 *
 * <p>These exercise {@code validateClaims} directly with a crafted, already
 * signature-verified payload, so they need no real Google token or network.
 */
class GoogleTokenVerifierServiceTest {

    private GoogleTokenVerifierService service;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties(
                "test-client-id.apps.googleusercontent.com", "vitapstudent.ac.in");
        service = new GoogleTokenVerifierService(props);
    }

    private Payload payload(String email, Boolean verified) {
        Payload p = new Payload();
        p.setSubject("google-sub-123");
        p.setEmail(email);
        p.setEmailVerified(verified);
        p.set("name", "Test Student");
        p.set("picture", "https://example.com/pic.jpg");
        return p;
    }

    @Test
    void acceptsVerifiedCollegeEmail() {
        VerifiedUser user = service.validateClaims(payload("student@vitapstudent.ac.in", true));

        assertThat(user.googleId()).isEqualTo("google-sub-123");
        assertThat(user.email()).isEqualTo("student@vitapstudent.ac.in");
        assertThat(user.name()).isEqualTo("Test Student");
        assertThat(user.picture()).isEqualTo("https://example.com/pic.jpg");
    }

    @Test
    void normalizesEmailToLowerCase() {
        VerifiedUser user = service.validateClaims(payload("Student@VITAPStudent.AC.IN", true));

        assertThat(user.email()).isEqualTo("student@vitapstudent.ac.in");
    }

    @Test
    void rejectsNonCollegeDomain() {
        assertThatThrownBy(() -> service.validateClaims(payload("someone@gmail.com", true)))
                .isInstanceOf(EmailDomainNotAllowedException.class);
    }

    @Test
    void rejectsLookalikeDomain() {
        // Must match the domain exactly, not merely end with it.
        assertThatThrownBy(() -> service.validateClaims(payload("a@evil-vitapstudent.ac.in", true)))
                .isInstanceOf(EmailDomainNotAllowedException.class);
        assertThatThrownBy(() -> service.validateClaims(payload("a@sub.vitapstudent.ac.in", true)))
                .isInstanceOf(EmailDomainNotAllowedException.class);
    }

    @Test
    void rejectsUnverifiedEmail() {
        assertThatThrownBy(() -> service.validateClaims(payload("student@vitapstudent.ac.in", false)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsMissingEmail() {
        assertThatThrownBy(() -> service.validateClaims(payload(null, true)))
                .isInstanceOf(InvalidTokenException.class);
    }
}
