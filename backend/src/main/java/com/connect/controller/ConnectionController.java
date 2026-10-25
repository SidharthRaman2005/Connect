package com.connect.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.ConnectionDto;
import com.connect.dto.ConnectionRequest;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.ConnectionService;

import jakarta.validation.Valid;

/**
 * Connection requests between students (M3.2): send, accept, reject, remove, and
 * list incoming/outgoing/accepted. All endpoints require a valid session JWT.
 */
@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /** Sends a connection request to another student. */
    @PostMapping
    public ConnectionDto send(@AuthenticationPrincipal AuthenticatedStudent principal,
                              @Valid @RequestBody ConnectionRequest request) {
        return connectionService.send(principal.id(), request.addresseeId());
    }

    /** Accepts a pending incoming request. */
    @PostMapping("/{id}/accept")
    public ConnectionDto accept(@AuthenticationPrincipal AuthenticatedStudent principal,
                                @PathVariable Long id) {
        return connectionService.respond(principal.id(), id, true);
    }

    /** Rejects a pending incoming request. */
    @PostMapping("/{id}/reject")
    public ConnectionDto reject(@AuthenticationPrincipal AuthenticatedStudent principal,
                                @PathVariable Long id) {
        return connectionService.respond(principal.id(), id, false);
    }

    /** Removes a connection or cancels a pending request. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedStudent principal,
                                       @PathVariable Long id) {
        connectionService.remove(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    /** Pending requests addressed to the caller. */
    @GetMapping("/incoming")
    public List<ConnectionDto> incoming(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return connectionService.incoming(principal.id());
    }

    /** Pending requests the caller has sent. */
    @GetMapping("/outgoing")
    public List<ConnectionDto> outgoing(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return connectionService.outgoing(principal.id());
    }

    /** The caller's accepted connections. */
    @GetMapping("/accepted")
    public List<ConnectionDto> accepted(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return connectionService.accepted(principal.id());
    }
}
