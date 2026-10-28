package com.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequest(
        @NotNull Long recipientId,
        @NotBlank @Size(max = 2000) String content) {}
