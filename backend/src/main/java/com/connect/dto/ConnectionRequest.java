package com.connect.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body for sending a connection request: the id of the student to connect with.
 */
public record ConnectionRequest(

        @NotNull(message = "addresseeId is required.")
        Long addresseeId) {
}
