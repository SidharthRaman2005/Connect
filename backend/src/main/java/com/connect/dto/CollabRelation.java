package com.connect.dto;

/**
 * The viewer's relationship to a collab post, from their point of view.
 */
public enum CollabRelation {
    /** The viewer created this post. */
    OWNER,
    /** The viewer has a pending request to join. */
    REQUESTED,
    /** The viewer's request was accepted — they're in. */
    MEMBER,
    /** No request yet. */
    NONE
}
