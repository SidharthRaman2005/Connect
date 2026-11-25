package com.connect.repository;

import java.util.List;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

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

    List<CollabPost> findByStatusInOrderByCreatedAtDesc(List<CollabStatus> statuses);

    List<CollabPost> findByStatusOrderByCreatedAtDesc(CollabStatus status);

    List<CollabPost> findByStatusAndCreatedAtBefore(CollabStatus status, Instant cutoff);

    List<CollabPost> findByStatusInAndTerminalAtBefore(List<CollabStatus> statuses, Instant cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CollabPost p where p.id = :id")
    java.util.Optional<CollabPost> findByIdForUpdate(@Param("id") Long id);
}
