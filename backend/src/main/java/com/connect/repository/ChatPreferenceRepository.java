package com.connect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connect.entity.ChatPreference;

public interface ChatPreferenceRepository extends JpaRepository<ChatPreference, Long> {
    Optional<ChatPreference> findByStudentIdAndOtherId(Long studentId, Long otherId);
}
