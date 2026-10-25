package com.connect.dto;

/**
 * The connection relationship between the current student and another, from the
 * current student's point of view. Drives which action a card/profile shows.
 */
public enum RelationState {
    /** No connection or pending request either way. */
    NONE,
    /** The current student has a pending request awaiting the other's response. */
    PENDING_OUTGOING,
    /** The other student has sent the current student a request to respond to. */
    PENDING_INCOMING,
    /** The two students are connected. */
    CONNECTED
}
