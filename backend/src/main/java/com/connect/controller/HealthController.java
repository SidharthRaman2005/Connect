package com.connect.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple health-check endpoint used to confirm the backend is up and reachable
 * from the frontend. A richer health report is also exposed by Spring Boot
 * Actuator at {@code /actuator/health}.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "connect-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
