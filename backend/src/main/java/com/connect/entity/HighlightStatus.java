package com.connect.entity;

/**
 * Optional lifecycle state of a {@link Highlight}. An achievement is typically
 * {@link #COMPLETED}; an ongoing piece of work can be {@link #IN_PROGRESS}.
 */
public enum HighlightStatus {
    IDEA,
    IN_PROGRESS,
    COMPLETED
}
