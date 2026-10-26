package com.connect.dto;

import java.util.List;

import com.connect.entity.HighlightStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for creating or updating a highlight (M4.1). Tags and link are optional;
 * blanks are normalised away server-side.
 */
public record SaveHighlightRequest(

        @NotBlank(message = "Title is required.")
        @Size(max = 150, message = "Title must be at most 150 characters.")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters.")
        String description,

        List<String> tags,

        @Size(max = 500, message = "Link must be at most 500 characters.")
        String link,

        @NotNull(message = "Status is required.")
        HighlightStatus status) {
}
