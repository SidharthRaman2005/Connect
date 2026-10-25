package com.connect.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A predefined, admin-curated label a student can add to their profile — a
 * skill, interest, or club (M2.2).
 *
 * <p>Tags are not free-form: students choose from the {@link TagStatus#APPROVED}
 * catalog and cannot create custom ones. The name is unique within a category,
 * so the same word may exist as both a skill and an interest but not twice as a
 * skill. Attaching tags to profiles is M2.3.
 */
@Entity
@Table(
        name = "tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tag_category_name",
                columnNames = {"category", "name"}))
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private TagCategory category;

    /** Display name, e.g. "Machine Learning". Unique within its category. */
    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TagStatus status = TagStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Tag() {
        // Required by JPA.
    }

    public Tag(TagCategory category, String name, TagStatus status) {
        this.category = category;
        this.name = name;
        this.status = status;
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

    public TagCategory getCategory() {
        return category;
    }

    public void setCategory(TagCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TagStatus getStatus() {
        return status;
    }

    public void setStatus(TagStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
