package com.connect.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Per-user state for a conversation: mute, archive and "clear history" timestamp.
 * Preferences are private to the viewer and never affect the other participant.
 */
@Entity
@Table(name = "chat_preferences", uniqueConstraints =
        @UniqueConstraint(name = "uk_chat_preference_student_other", columnNames = {"student_id", "other_id"}))
public class ChatPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "other_id", nullable = false)
    private Long otherId;

    @Column(nullable = false)
    private boolean muted = false;

    @Column(nullable = false)
    private boolean archived = false;

    /** Messages sent at or before this instant are hidden from this viewer. */
    @Column(name = "cleared_at")
    private Instant clearedAt;

    protected ChatPreference() {}

    public ChatPreference(Long studentId, Long otherId) {
        this.studentId = studentId;
        this.otherId = otherId;
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public Long getOtherId() { return otherId; }
    public boolean isMuted() { return muted; }
    public boolean isArchived() { return archived; }
    public Instant getClearedAt() { return clearedAt; }

    public void apply(Boolean mute, Boolean archive) {
        if (mute != null) this.muted = mute;
        if (archive != null) this.archived = archive;
    }

    public void clearHistory() {
        this.clearedAt = Instant.now();
        this.archived = true;
    }
}
