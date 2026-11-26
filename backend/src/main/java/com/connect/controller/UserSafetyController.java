package com.connect.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.connect.dto.StudentBrief;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.UserSafetyService;

@RestController
@RequestMapping("/api/users")
public class UserSafetyController {
    private final UserSafetyService safetyService;
    public UserSafetyController(UserSafetyService safetyService) { this.safetyService = safetyService; }

    @GetMapping("/blocked")
    public List<StudentBrief> blocked(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return safetyService.blocked(principal.id());
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<Void> block(@AuthenticationPrincipal AuthenticatedStudent principal, @PathVariable Long id) {
        safetyService.block(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/block")
    public ResponseEntity<Void> unblock(@AuthenticationPrincipal AuthenticatedStudent principal, @PathVariable Long id) {
        safetyService.unblock(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
