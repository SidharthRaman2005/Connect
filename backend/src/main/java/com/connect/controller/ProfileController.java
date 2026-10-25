package com.connect.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.connect.dto.ProfileDto;
import com.connect.dto.PublicProfileDto;
import com.connect.dto.UpdateProfileRequest;
import com.connect.dto.UpdateTagsRequest;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.FileStorageService;
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
    private final FileStorageService fileStorageService;

    public ProfileController(ProfileService profileService, FileStorageService fileStorageService) {
        this.profileService = profileService;
        this.fileStorageService = fileStorageService;
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

    /** Uploads a profile picture from the device; returns the updated profile. */
    @PostMapping("/me/picture")
    public ProfileDto uploadMyPicture(@AuthenticationPrincipal AuthenticatedStudent principal,
                                      @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeProfilePicture(file);
        return profileService.updatePicture(principal.id(), url);
    }

    /** Removes the caller's profile picture. */
    @DeleteMapping("/me/picture")
    public ProfileDto deleteMyPicture(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return profileService.clearPicture(principal.id());
    }

    /** Replaces the caller's tag set; returns the updated full profile. */
    @PutMapping("/me/tags")
    public ProfileDto updateMyTags(@AuthenticationPrincipal AuthenticatedStudent principal,
                                   @Valid @RequestBody UpdateTagsRequest request) {
        return profileService.updateTags(principal.id(), request.tagIds());
    }

    /** Another student's public profile; 403 if it is private. */
    @GetMapping("/{id}")
    public PublicProfileDto getProfile(@AuthenticationPrincipal AuthenticatedStudent principal,
                                       @PathVariable Long id) {
        return profileService.getProfile(principal.id(), id);
    }
}
