package com.connect.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.connect.entity.BlogComment;

public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {
    List<BlogComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    Optional<BlogComment> findByIdAndPostId(Long id, Long postId);
    void deleteByPostId(Long postId);
}
