package com.connect.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.PageResponse;
import com.connect.dto.StudentCardDto;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.DiscoveryService;

/**
 * Student discovery and search (M3.1). The feed is tag-ranked; search filters by
 * name, branch, batch, and tags. Only PUBLIC students (other than the caller)
 * are returned. Requires a valid session JWT.
 */
@RestController
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /** Suggested students, ranked by shared tags. */
    @GetMapping("/api/discover")
    public List<StudentCardDto> discover(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return discoveryService.discover(principal.id());
    }

    /** Filtered, paginated people search. All filters are optional. */
    @GetMapping("/api/students/search")
    public PageResponse<StudentCardDto> search(
            @AuthenticationPrincipal AuthenticatedStudent principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) Integer batch,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return discoveryService.search(principal.id(), q, branch, batch, tagIds, page, size);
    }
}
