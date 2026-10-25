package com.connect.entity;

/**
 * State of a {@link Connection} request between two students (M3.2).
 */
public enum ConnectionStatus {
    /** Request sent, awaiting the addressee's response. */
    PENDING,
    /** Request accepted; the two students are connected. */
    ACCEPTED,
    /** Request declined by the addressee. */
    REJECTED
}
