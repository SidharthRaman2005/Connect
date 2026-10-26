package com.connect.dto;

import java.time.Instant;
import java.util.List;

import com.connect.entity.CollabPost;
import com.connect.entity.CollabStatus;
import com.connect.entity.CollabType;

/**
 * A collab post as its owner manages it: includes the join requests (pending
 * and accepted) so the owner can accept members and see who's in.
 */
public record OwnerCollabPostDto(
        Long id,
        String title,
        String description,
        CollabType type,
        CollabStatus status,
        Instant createdAt,
        int acceptedCount,
        List<CollabRequestDto> requests) {

    public static OwnerCollabPostDto of(CollabPost p, List<CollabRequestDto> requests, int acceptedCount) {
        return new OwnerCollabPostDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getType(),
                p.getStatus(),
                p.getCreatedAt(),
                acceptedCount,
                requests);
    }
}
