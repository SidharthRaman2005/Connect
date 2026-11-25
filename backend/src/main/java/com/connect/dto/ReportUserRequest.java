package com.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportUserRequest(@NotBlank @Size(max = 80) String reason, @Size(max = 2000) String details) {}
