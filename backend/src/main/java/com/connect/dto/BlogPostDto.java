package com.connect.dto;

import java.time.Instant;

import com.connect.entity.BlogPost;

public record BlogPostDto(
        Long id,
        StudentBrief author,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static BlogPostDto from(BlogPost post) {
        return new BlogPostDto(
                post.getId(),
                StudentBrief.from(post.getAuthor()),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
