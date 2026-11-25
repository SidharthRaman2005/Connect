package com.connect.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "blog_comments", indexes = @Index(name = "idx_blog_comments_post_created", columnList = "post_id,created_at"))
public class BlogComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false)
    private Student author;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BlogComment() {}
    public BlogComment(BlogPost post, Student author, String content) { this.post = post; this.author = author; this.content = content; }
    @PrePersist void createTimestamp() { if (createdAt == null) createdAt = Instant.now(); }
    public Long getId() { return id; }
    public BlogPost getPost() { return post; }
    public Student getAuthor() { return author; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
