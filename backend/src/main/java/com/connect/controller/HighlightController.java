package com.connect.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.HighlightDto;
import com.connect.dto.SaveHighlightRequest;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.HighlightService;

import jakarta.validation.Valid;

/**
 * Highlight endpoints (M4.1). A student manages their own highlights; any
 * signed-in student can view another's.
 */
@RestController
public class HighlightController {

    private final HighlightService highlightService;

    public HighlightController(HighlightService highlightService) {
        this.highlightService = highlightService;
    }

    @GetMapping("/api/highlights/me")
    public List<HighlightDto> mine(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return highlightService.getMyHighlights(principal.id());
    }

    @GetMapping("/api/students/{id}/highlights")
    public List<HighlightDto> forStudent(@PathVariable Long id) {
        return highlightService.getHighlightsForStudent(id);
    }

    @PostMapping("/api/highlights")
    public HighlightDto create(@AuthenticationPrincipal AuthenticatedStudent principal,
                               @Valid @RequestBody SaveHighlightRequest request) {
        return highlightService.create(principal.id(), request);
    }

    @PutMapping("/api/highlights/{id}")
    public HighlightDto update(@AuthenticationPrincipal AuthenticatedStudent principal,
                               @PathVariable Long id,
                               @Valid @RequestBody SaveHighlightRequest request) {
        return highlightService.update(principal.id(), id, request);
    }

    @DeleteMapping("/api/highlights/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedStudent principal,
                                       @PathVariable Long id) {
        highlightService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
