package com.connect.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.ProfileDto;
import com.connect.dto.PublicProfileDto;
import com.connect.dto.UpdateProfileRequest;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.ProfileService;

import jakarta.validation.Valid;

/**
 * Student profile endpoints (M2.1).
 *
 * <p>{@code /me} reads and updates the caller's own profile; {@code /{id}}
 * returns another student's public profile, subject to their visibility
 * setting. All endpoints require a valid session JWT.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** The authenticated student's own, full profile. */
    @GetMapping("/me")
    public ProfileDto getMyProfile(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return profileService.getOwnProfile(principal.id());
    }

    /** Updates the caller's editable profile fields and completes onboarding. */
    @PutMapping("/me")
    public ProfileDto updateMyProfile(@AuthenticationPrincipal AuthenticatedStudent principal,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(principal.id(), request);
    }

    /** Another student's public profile; 403 if it is private. */
    @GetMapping("/{id}")
    public PublicProfileDto getProfile(@AuthenticationPrincipal AuthenticatedStudent principal,
                                       @PathVariable Long id) {
        return profileService.getProfile(principal.id(), id);
    }
}
