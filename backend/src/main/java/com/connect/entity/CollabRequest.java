package com.connect.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A student's request to join a {@link CollabPost} (M4.2). At most one request
 * per (post, applicant). When accepted, {@code seen} is reset so the applicant
 * can be notified that they're in.
 */
@Entity
@Table(
        name = "collab_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_collab_request_post_applicant",
                columnNames = {"post_id", "applicant_id"}))
public class CollabRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private CollabPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Student applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollabRequestStatus status = CollabRequestStatus.PENDING;

    /** False after acceptance until the applicant has seen the notification. */
    @Column(nullable = false)
    private boolean seen = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CollabRequest() {
        // Required by JPA.
    }

    public CollabRequest(CollabPost post, Student applicant) {
        this.post = post;
        this.applicant = applicant;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CollabPost getPost() {
        return post;
    }

    public Student getApplicant() {
        return applicant;
    }

    public CollabRequestStatus getStatus() {
        return status;
    }

    public void setStatus(CollabRequestStatus status) {
        this.status = status;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
