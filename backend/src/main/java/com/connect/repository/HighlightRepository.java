package com.connect.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connect.entity.Highlight;

/**
 * Data access for {@link Highlight}s.
 */
public interface HighlightRepository extends JpaRepository<Highlight, Long> {

    /** A student's highlights, most recently updated first. */
    List<Highlight> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
