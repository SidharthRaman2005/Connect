package com.connect.controller;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connect.security.AuthenticatedStudent;
import com.connect.service.ConnectionService;

/**
 * Development-only helpers for testing. Registered only when
 * {@code app.dev.seed-enabled=true} (the default); disable in production.
 */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "app.dev.seed-enabled", havingValue = "true", matchIfMissing = true)
public class DevController {

    private final ConnectionService connectionService;

    public DevController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /** Makes a few dummy students send the caller a pending connection request. */
    @PostMapping("/seed-incoming")
    public Map<String, Integer> seedIncoming(@AuthenticationPrincipal AuthenticatedStudent principal) {
        int created = connectionService.seedIncomingFor(principal.id(), 3);
        return Map.of("created", created);
    }
}
