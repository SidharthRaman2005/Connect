package com.connect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.NotificationInboxDto;
import com.connect.exception.BadRequestException;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.NotificationService;

/** Persisted notification inbox and per-student read state. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private static final int MAX_PAGE_SIZE = 100;
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationInboxDto inbox(@AuthenticationPrincipal AuthenticatedStudent principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page must be non-negative and size must be 1 to 100.");
        }
        return notificationService.inbox(principal.id(), page, size);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal AuthenticatedStudent principal,
                                         @PathVariable Long id) {
        notificationService.markRead(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthenticatedStudent principal) {
        notificationService.markAllRead(principal.id());
        return ResponseEntity.noContent().build();
    }
}
