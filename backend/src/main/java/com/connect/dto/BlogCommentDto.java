package com.connect.dto;

import java.time.Instant;
import com.connect.entity.BlogComment;

public record BlogCommentDto(Long id, Long postId, StudentBrief author, String content, Instant createdAt) {
    public static BlogCommentDto from(BlogComment comment) {
        return new BlogCommentDto(comment.getId(), comment.getPost().getId(),
                StudentBrief.from(comment.getAuthor()), comment.getContent(), comment.getCreatedAt());
    }
}
