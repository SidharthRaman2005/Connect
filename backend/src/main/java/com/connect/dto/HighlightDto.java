package com.connect.dto;

import java.time.Instant;
import java.util.List;

import com.connect.entity.Highlight;
import com.connect.entity.HighlightStatus;

/**
 * A highlight as returned to the frontend (M4.1).
 */
public record HighlightDto(
        Long id,
        Long ownerId,
        String title,
        String description,
        List<String> tags,
        String link,
        HighlightStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static HighlightDto from(Highlight h) {
        return new HighlightDto(
                h.getId(),
                h.getOwner().getId(),
                h.getTitle(),
                h.getDescription(),
                List.copyOf(h.getTags()),
                h.getLink(),
                h.getStatus(),
                h.getCreatedAt(),
                h.getUpdatedAt());
    }
}
