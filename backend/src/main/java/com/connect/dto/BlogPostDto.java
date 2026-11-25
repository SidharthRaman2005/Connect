package com.connect.dto;

import java.time.Instant;
import java.util.List;

import com.connect.entity.BlogPost;

public record BlogPostDto(
        Long id,
        StudentBrief author,
        String title,
        String content,
        List<String> tags,
        long likeCount,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt) {

    public static BlogPostDto from(BlogPost post) {
        return from(post, 0L, false);
    }

    public static BlogPostDto from(BlogPost post, long likeCount, boolean likedByMe) {
        return new BlogPostDto(
                post.getId(),
                StudentBrief.from(post.getAuthor()),
                post.getTitle(),
                post.getContent(),
                post.getTags().stream().toList(),
                likeCount,
                likedByMe,
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
