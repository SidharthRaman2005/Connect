package com.connect.dto;

import com.connect.entity.ProfileVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The editable fields of a student's profile, submitted during onboarding and
 * from the profile edit screen. Identity fields (name, picture, email) come
 * from Google and are not editable here; {@code batch} is derived from the
 * email.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Branch is required.")
        @Size(max = 100, message = "Branch must be at most 100 characters.")
        String branch,

        @Size(max = 1000, message = "Bio must be at most 1000 characters.")
        String bio,

        @NotNull(message = "Visibility is required.")
        ProfileVisibility visibility) {
}
