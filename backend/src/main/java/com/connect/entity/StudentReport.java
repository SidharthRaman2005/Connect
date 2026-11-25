package com.connect.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "student_reports", indexes = @Index(name = "idx_student_reports_status_created", columnList = "status,created_at"))
public class StudentReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reporter_id", nullable = false)
    private Student reporter;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "target_id", nullable = false)
    private Student target;
    @Column(nullable = false, length = 80)
    private String reason;
    @Column(length = 2000)
    private String details;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    protected StudentReport() {}
    public StudentReport(Student reporter, Student target, String reason, String details) {
        this.reporter = reporter; this.target = target; this.reason = reason; this.details = details;
    }
    @PrePersist void setCreatedAt() { if (createdAt == null) createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Student getTarget() { return target; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public ReportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
