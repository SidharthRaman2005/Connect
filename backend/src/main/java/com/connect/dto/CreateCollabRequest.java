package com.connect.dto;

import com.connect.entity.CollabType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Body for publishing a collab post (M4.2).
 */
public record CreateCollabRequest(

        @NotBlank(message = "Title is required.")
        @Size(max = 150, message = "Title must be at most 150 characters.")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters.")
        String description,

        @NotNull(message = "Type is required.")
        CollabType type,

        @Min(value = 1, message = "At least one teammate must be requested.")
        @Max(value = 50, message = "A collab can request up to 50 teammates.")
        int requiredPeople) {
}
