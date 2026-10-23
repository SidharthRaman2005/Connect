package com.connect.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * A student account, created on first Google sign-in and reused afterwards.
 *
 * <p>M1.2 holds only identity fields sourced from Google plus bookkeeping
 * timestamps. Profile fields (branch, year, bio, skills, ...) are added in the
 * Phase 2 profile modules. {@code profileCompleted} drives the first-time
 * onboarding redirect.
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable Google account id (the ID token "sub" claim). */
    @Column(name = "google_id", nullable = false, unique = true, updatable = false)
    private String googleId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String name;

    @Column(name = "picture_url")
    private String pictureUrl;

    /** False until the student finishes onboarding (set in Phase 2). */
    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public Student() {
        // Required by JPA; public so it can be constructed in tests/services.
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

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
