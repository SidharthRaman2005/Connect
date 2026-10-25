package com.connect.entity;

/**
 * Moderation state of a {@link Tag}.
 *
 * <p>Only {@link #APPROVED} tags are visible to students and attachable to
 * profiles. The predefined catalog is seeded as APPROVED. PENDING and REJECTED
 * exist so the admin tag-management workflow (M6.1) can slot in without a schema
 * change; there is no student-facing way to create tags in M2.2.
 */
public enum TagStatus {
    PENDING,
    APPROVED,
    REJECTED
}
