package com.connect.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A persisted direct message between two connected students. */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_sender_recipient_sent", columnList = "sender_id,recipient_id,sent_at"),
        @Index(name = "idx_chat_recipient_sender_read", columnList = "recipient_id,sender_id,read_at")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Student sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Student recipient;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected ChatMessage() {}

    public ChatMessage(Student sender, Student recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        if (sentAt == null) sentAt = Instant.now();
    }

    public Long getId() { return id; }
    public Student getSender() { return sender; }
    public Student getRecipient() { return recipient; }
    public String getContent() { return content; }
    public Instant getSentAt() { return sentAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
