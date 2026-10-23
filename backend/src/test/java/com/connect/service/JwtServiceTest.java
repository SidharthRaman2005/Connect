package com.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.connect.config.AuthProperties;
import com.connect.entity.Student;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-bytes-long!!";

    private JwtService jwtService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties(
                "client-id", "vitapstudent.ac.in", SECRET, 3600);
        jwtService = new JwtService(props);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void issuesSignedTokenWithSubjectEmailAndExpiry() {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(42L);
        when(student.getEmail()).thenReturn("student@vitapstudent.ac.in");

        String token = jwtService.issueToken(student);

        Jws<Claims> parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        Claims claims = parsed.getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("student@vitapstudent.ac.in");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void parseAcceptsTokenItIssued() {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(7L);
        when(student.getEmail()).thenReturn("x@vitapstudent.ac.in");

        String token = jwtService.issueToken(student);

        assertThat(jwtService.parse(token).getPayload().getSubject()).isEqualTo("7");
    }

    @Test
    void parseRejectsTamperedToken() {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(7L);
        when(student.getEmail()).thenReturn("x@vitapstudent.ac.in");

        String tampered = jwtService.issueToken(student) + "tamper";

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }
}
