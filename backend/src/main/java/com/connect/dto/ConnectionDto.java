package com.connect.dto;

import java.time.Instant;

import com.connect.entity.Connection;
import com.connect.entity.ConnectionStatus;
import com.connect.entity.Student;

/**
 * A connection as seen by one of its two students. {@code direction} is relative
 * to the viewer (INCOMING = the viewer is the addressee), and {@code other} is
 * the student on the far side.
 */
public record ConnectionDto(
        Long id,
        ConnectionStatus status,
        String direction,
        StudentCardDto other,
        Instant createdAt) {

    public static ConnectionDto forViewer(Connection c, Long viewerId, RelationState relation) {
        boolean incoming = c.getAddressee().getId().equals(viewerId);
        Student other = incoming ? c.getRequester() : c.getAddressee();
        return new ConnectionDto(
                c.getId(),
                c.getStatus(),
                incoming ? "INCOMING" : "OUTGOING",
                StudentCardDto.of(other, 0, relation),
                c.getCreatedAt());
    }
}
