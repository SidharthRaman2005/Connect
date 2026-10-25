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
 * A directed connection request between two students (M3.2): the
 * {@code requester} asks to connect with the {@code addressee}. Once
 * {@link ConnectionStatus#ACCEPTED} the two are mutually connected.
 *
 * <p>At most one row may exist per (requester, addressee) ordered pair; the
 * service prevents duplicate or reverse requests between the same two students.
 */
@Entity
@Table(
        name = "connections",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_connection_requester_addressee",
                columnNames = {"requester_id", "addressee_id"}))
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Student requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private Student addressee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConnectionStatus status = ConnectionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Connection() {
        // Required by JPA.
    }

    public Connection(Student requester, Student addressee, ConnectionStatus status) {
        this.requester = requester;
        this.addressee = addressee;
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

    public Student getRequester() {
        return requester;
    }

    public Student getAddressee() {
        return addressee;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
