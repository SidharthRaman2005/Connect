package com.connect.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.connect.config.AuthProperties;
import com.connect.entity.Student;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues signed session JWTs for authenticated students.
 *
 * <p>Validation of these tokens (an auth filter on protected endpoints) is
 * added in M1.3; this module only mints them.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(AuthProperties authProperties) {
        String secret = authProperties.jwtSecret();
        if (secret == null || secret.isBlank()) {
            this.key = Jwts.SIG.HS256.key().build();
            log.warn("No app.auth.jwt-secret configured; generated an ephemeral signing key. "
                    + "Sessions will not survive a restart. Set JWT_SECRET for stable sessions.");
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expirationSeconds = authProperties.jwtExpirationSeconds();
    }

    /**
     * Mint a JWT whose subject is the student's id, with the email as a claim.
     */
    public String issueToken(Student student) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .subject(String.valueOf(student.getId()))
                .claim("email", student.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /** Seconds until an issued token expires (useful for the client). */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
