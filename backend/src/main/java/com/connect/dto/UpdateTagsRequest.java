package com.connect.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * Replaces the set of tags on the caller's profile (M2.3). The given ids become
 * the profile's complete tag set, so an empty list clears all tags. Every id
 * must refer to an existing, approved tag.
 */
public record UpdateTagsRequest(

        @NotNull(message = "tagIds is required (use an empty list to clear).")
        List<Long> tagIds) {
}
