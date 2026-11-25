package com.connect.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "blog_likes", uniqueConstraints = @UniqueConstraint(name = "uk_blog_like_post_student", columnNames = {"post_id", "student_id"}))
public class BlogLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    protected BlogLike() {}
    public BlogLike(BlogPost post, Student student) { this.post = post; this.student = student; }
}
