package com.connect.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/** Short-lived, single-use bridge from an authenticated HTTP session to WebSocket. */
@Service
public class WebSocketTicketService {
    private static final long LIFETIME_SECONDS = 30;
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String issue(Long studentId) {
        Instant expiry = Instant.now().plusSeconds(LIFETIME_SECONDS);
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(Instant.now()));
        String value = UUID.randomUUID().toString();
        tickets.put(value, new Ticket(studentId, expiry));
        return value;
    }

    public Long consume(String value) {
        if (value == null || value.isBlank()) return null;
        Ticket ticket = tickets.remove(value);
        return ticket != null && ticket.expiresAt().isAfter(Instant.now()) ? ticket.studentId() : null;
    }

    public long lifetimeSeconds() { return LIFETIME_SECONDS; }

    private record Ticket(Long studentId, Instant expiresAt) {}
}
