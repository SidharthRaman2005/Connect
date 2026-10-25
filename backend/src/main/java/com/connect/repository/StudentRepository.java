package com.connect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;

/**
 * Data access for {@link Student} accounts.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByGoogleId(String googleId);

    Optional<Student> findByEmail(String email);

    /** Discovery/search candidates: a given visibility, excluding one student. */
    List<Student> findByProfileVisibilityAndIdNot(ProfileVisibility visibility, Long id);

    /** Dummy accounts seeded for development (googleId starts with "dummy-"). */
    List<Student> findByGoogleIdStartingWith(String prefix);
}
