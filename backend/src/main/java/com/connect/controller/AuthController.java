package com.connect.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.GoogleLoginRequest;
import com.connect.dto.VerifiedUser;
import com.connect.service.GoogleTokenVerifierService;

import jakarta.validation.Valid;

/**
 * Authentication endpoints.
 *
 * <p>M1.1: {@code POST /api/auth/google} validates a Google ID token and
 * returns the verified college identity. Creating/authenticating a Student and
 * issuing a session token are added in M1.2 / M1.3.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleTokenVerifierService tokenVerifierService;

    public AuthController(GoogleTokenVerifierService tokenVerifierService) {
        this.tokenVerifierService = tokenVerifierService;
    }

    @PostMapping("/google")
    public VerifiedUser google(@Valid @RequestBody GoogleLoginRequest request) {
        return tokenVerifierService.verify(request.idToken());
    }
}
