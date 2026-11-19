package com.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for editing an existing chat message. */
public record UpdateMessageRequest(
        @NotBlank(message = "Message cannot be empty.")
        @Size(max = 2000, message = "Message must be 2000 characters or fewer.")
        String content) {}
