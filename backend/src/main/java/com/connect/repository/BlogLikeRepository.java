package com.connect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.connect.entity.BlogLike;

public interface BlogLikeRepository extends JpaRepository<BlogLike, Long> {
    boolean existsByPostIdAndStudentId(Long postId, Long studentId);
    long countByPostId(Long postId);
    void deleteByPostIdAndStudentId(Long postId, Long studentId);
    void deleteByPostId(Long postId);
}
