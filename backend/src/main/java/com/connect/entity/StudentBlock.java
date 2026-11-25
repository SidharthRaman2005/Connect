package com.connect.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "student_blocks", uniqueConstraints = @UniqueConstraint(name = "uk_student_block_pair", columnNames = {"blocker_id", "blocked_id"}))
public class StudentBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "blocker_id", nullable = false)
    private Student blocker;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "blocked_id", nullable = false)
    private Student blocked;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    protected StudentBlock() {}
    public StudentBlock(Student blocker, Student blocked) { this.blocker = blocker; this.blocked = blocked; }
    @PrePersist void setCreatedAt() { if (createdAt == null) createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Student getBlocked() { return blocked; }
    public Instant getCreatedAt() { return createdAt; }
}
