package com.connect.entity;

/**
 * State of a request to join a {@link CollabPost} (M4.2). There is no rejected
 * state by design — the owner simply accepts whom they want and can close the
 * post when done.
 */
public enum CollabRequestStatus {
    PENDING,
    ACCEPTED
}
