package com.connect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.connect.entity.Student;

/**
 * Data access for {@link Student} accounts.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByGoogleId(String googleId);

    Optional<Student> findByEmail(String email);
}
