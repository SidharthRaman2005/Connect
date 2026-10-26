package com.connect.dto;

import com.connect.entity.CollabRequest;

/**
 * A "you're in" notification for a student whose collab request was accepted.
 */
public record CollabNotificationDto(
        Long requestId,
        Long postId,
        String postTitle,
        String ownerName) {

    public static CollabNotificationDto from(CollabRequest r) {
        return new CollabNotificationDto(
                r.getId(),
                r.getPost().getId(),
                r.getPost().getTitle(),
                r.getPost().getOwner().getName());
    }
}
