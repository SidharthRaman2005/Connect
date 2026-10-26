package com.connect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connect.entity.CollabPost;
import com.connect.entity.CollabStatus;

/**
 * Data access for {@link CollabPost}s.
 */
public interface CollabPostRepository extends JpaRepository<CollabPost, Long> {

    /** The recent-posts feed: newest open posts first. */
    List<CollabPost> findTop50ByStatusOrderByCreatedAtDesc(CollabStatus status);

    /** A student's own posts, newest first. */
    List<CollabPost> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
