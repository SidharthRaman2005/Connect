package com.connect.dto;

import java.time.Instant;

import com.connect.entity.CollabRequest;
import com.connect.entity.CollabRequestStatus;

/**
 * A request to join a collab, as the owner sees it.
 */
public record CollabRequestDto(
        Long id,
        StudentBrief applicant,
        CollabRequestStatus status,
        Instant createdAt) {

    public static CollabRequestDto from(CollabRequest r) {
        return new CollabRequestDto(
                r.getId(),
                StudentBrief.from(r.getApplicant()),
                r.getStatus(),
                r.getCreatedAt());
    }
}
