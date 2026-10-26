package com.connect.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.CollabNotificationDto;
import com.connect.dto.CollabPostDto;
import com.connect.dto.CollabRequestDto;
import com.connect.dto.CreateCollabRequest;
import com.connect.dto.OwnerCollabPostDto;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.CollabService;

import jakarta.validation.Valid;

/**
 * Collaboration recruitment endpoints (M4.2): the recent-posts feed, publishing,
 * requesting to join, owner acceptance, closing, and "you're in" notifications.
 * All require a valid session JWT.
 */
@RestController
@RequestMapping("/api/collabs")
public class CollabController {

    private final CollabService collabService;

    public CollabController(CollabService collabService) {
        this.collabService = collabService;
    }

    /** Recent open collab posts. */
    @GetMapping
    public List<CollabPostDto> feed(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return collabService.feed(principal.id());
    }

    /** Publishes a new collab post. */
    @PostMapping
    public CollabPostDto create(@AuthenticationPrincipal AuthenticatedStudent principal,
                                @Valid @RequestBody CreateCollabRequest request) {
        return collabService.create(principal.id(), request);
    }

    /** The caller's own posts with their join requests. */
    @GetMapping("/me")
    public List<OwnerCollabPostDto> myPosts(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return collabService.myPosts(principal.id());
    }

    /** Closes one of the caller's posts. */
    @PostMapping("/{id}/close")
    public OwnerCollabPostDto close(@AuthenticationPrincipal AuthenticatedStudent principal,
                                    @PathVariable Long id) {
        return collabService.close(principal.id(), id);
    }

    /** Sends a request to join a post. */
    @PostMapping("/{id}/requests")
    public CollabRequestDto requestToJoin(@AuthenticationPrincipal AuthenticatedStudent principal,
                                          @PathVariable Long id) {
        return collabService.requestToJoin(principal.id(), id);
    }

    /** Owner accepts a join request. */
    @PostMapping("/requests/{requestId}/accept")
    public CollabRequestDto accept(@AuthenticationPrincipal AuthenticatedStudent principal,
                                   @PathVariable Long requestId) {
        return collabService.accept(principal.id(), requestId);
    }

    /** The caller's "you're in" notifications. */
    @GetMapping("/notifications")
    public List<CollabNotificationDto> notifications(
            @AuthenticationPrincipal AuthenticatedStudent principal) {
        return collabService.notifications(principal.id());
    }

    /** Marks the caller's collab notifications as seen. */
    @PostMapping("/notifications/seen")
    public ResponseEntity<Void> markSeen(@AuthenticationPrincipal AuthenticatedStudent principal) {
        collabService.markNotificationsSeen(principal.id());
        return ResponseEntity.noContent().build();
    }
}
