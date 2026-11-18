package com.connect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connect.entity.BlogPost;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    List<BlogPost> findTop50ByOrderByCreatedAtDesc();
    List<BlogPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
