package com.connect.dto;

import java.time.Instant;

import com.connect.entity.CollabPost;
import com.connect.entity.CollabStatus;
import com.connect.entity.CollabType;

/**
 * A collab post as shown in the recent-posts feed, with the viewer's relation
 * to it so the UI can show the right action.
 */
public record CollabPostDto(
        Long id,
        String title,
        String description,
        CollabType type,
        CollabStatus status,
        StudentBrief owner,
        Instant createdAt,
        CollabRelation relation) {

    public static CollabPostDto of(CollabPost p, CollabRelation relation) {
        return new CollabPostDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getType(),
                p.getStatus(),
                StudentBrief.from(p.getOwner()),
                p.getCreatedAt(),
                relation);
    }
}
