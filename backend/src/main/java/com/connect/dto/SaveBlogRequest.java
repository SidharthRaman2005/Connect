package com.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveBlogRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 180, message = "Title must be at most 180 characters.")
        String title,

        @NotBlank(message = "Blog content is required.")
        @Size(max = 10000, message = "Blog content must be at most 10000 characters.")
        String content,

        @Size(max = 8, message = "Add at most 8 tags.")
        List<@Size(max = 40, message = "Tags must be at most 40 characters.") String> tags) {
}
