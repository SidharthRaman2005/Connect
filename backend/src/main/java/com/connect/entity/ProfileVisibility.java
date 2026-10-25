package com.connect.entity;

/**
 * Who may view a student's full profile.
 *
 * <p>M2.1 supports PUBLIC and PRIVATE. CONNECTIONS_ONLY is planned for when
 * connections exist (M3.2).
 */
public enum ProfileVisibility {
    PUBLIC,
    PRIVATE
}
