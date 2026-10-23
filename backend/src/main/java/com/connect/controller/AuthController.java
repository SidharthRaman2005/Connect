package com.connect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.AuthResponse;
import com.connect.dto.GoogleLoginRequest;
import com.connect.dto.StudentDto;
import com.connect.dto.VerifiedUser;
import com.connect.entity.Student;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.GoogleTokenVerifierService;
import com.connect.service.JwtService;
import com.connect.service.StudentService;

import jakarta.validation.Valid;

/**
 * Authentication endpoints.
 *
 * <p>M1.2: {@code POST /api/auth/google} verifies the Google ID token, creates
 * or authenticates the Student, and returns a session JWT plus the student's
 * profile. Securing protected endpoints with that JWT and logout come in M1.3.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleTokenVerifierService tokenVerifierService;
    private final StudentService studentService;
    private final JwtService jwtService;

    public AuthController(GoogleTokenVerifierService tokenVerifierService,
                          StudentService studentService,
                          JwtService jwtService) {
        this.tokenVerifierService = tokenVerifierService;
        this.studentService = studentService;
        this.jwtService = jwtService;
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        VerifiedUser verifiedUser = tokenVerifierService.verify(request.idToken());
        StudentService.AuthResult result = studentService.createOrAuthenticate(verifiedUser);
        String token = jwtService.issueToken(result.student());
        return new AuthResponse(token, result.newAccount(), StudentDto.from(result.student()));
    }

    /** Returns the currently authenticated student. Requires a valid JWT. */
    @GetMapping("/me")
    public StudentDto me(@AuthenticationPrincipal AuthenticatedStudent principal) {
        Student student = studentService.getById(principal.id());
        return StudentDto.from(student);
    }

    /**
     * Logout. With stateless JWTs there is no server session to destroy, so the
     * client simply discards its token. The endpoint exists for symmetry and as
     * a hook for future server-side revocation.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
