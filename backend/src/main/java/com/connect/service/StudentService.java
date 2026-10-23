package com.connect.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connect.dto.VerifiedUser;
import com.connect.entity.Student;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.StudentRepository;

/**
 * Creates and authenticates {@link Student} accounts from a verified Google
 * identity.
 */
@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Result of a login: the (new or existing) student, and whether the account
     * was just created so the frontend can route first-timers to onboarding.
     */
    public record AuthResult(Student student, boolean newAccount) {
    }

    /**
     * Finds the student by Google id and authenticates them, or creates a new
     * account on first login. Refreshes the display name / picture from Google
     * and records the login time.
     */
    @Transactional
    public AuthResult createOrAuthenticate(VerifiedUser verifiedUser) {
        Optional<Student> existing = studentRepository.findByGoogleId(verifiedUser.googleId());

        Student student;
        boolean newAccount;
        if (existing.isPresent()) {
            student = existing.get();
            newAccount = false;
        } else {
            student = new Student();
            student.setGoogleId(verifiedUser.googleId());
            student.setEmail(verifiedUser.email());
            newAccount = true;
        }

        // Keep the name/picture in sync with Google on every login.
        student.setName(verifiedUser.name());
        student.setPictureUrl(verifiedUser.picture());
        student.setLastLoginAt(Instant.now());

        return new AuthResult(studentRepository.save(student), newAccount);
    }

    /**
     * Loads a student by id.
     *
     * @throws ResourceNotFoundException if no such student exists
     */
    @Transactional(readOnly = true)
    public Student getById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }
}
