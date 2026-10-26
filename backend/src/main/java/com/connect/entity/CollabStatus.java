package com.connect.entity;

/**
 * Whether a {@link CollabPost} is still accepting collaborators (M4.2).
 */
public enum CollabStatus {
    /** Visible in the feed and accepting requests. */
    OPEN,
    /** Closed by the owner (e.g. the team is full); no new requests. */
    CLOSED
}
