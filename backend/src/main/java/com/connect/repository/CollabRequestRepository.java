package com.connect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connect.entity.CollabRequest;
import com.connect.entity.CollabRequestStatus;

/**
 * Data access for {@link CollabRequest}s.
 */
public interface CollabRequestRepository extends JpaRepository<CollabRequest, Long> {

    boolean existsByPostIdAndApplicantId(Long postId, Long applicantId);

    List<CollabRequest> findByPostIdOrderByCreatedAtDesc(Long postId);

    /** The caller's own requests, to compute their relation to feed posts. */
    List<CollabRequest> findByApplicantId(Long applicantId);

    /** Accepted-but-unseen requests for a student — their collab notifications. */
    List<CollabRequest> findByApplicantIdAndStatusAndSeenFalse(Long applicantId, CollabRequestStatus status);

    int countByPostIdAndStatus(Long postId, CollabRequestStatus status);
}
