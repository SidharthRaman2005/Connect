package com.connect.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.connect.entity.Student;

@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    private Student student(String googleId, String email) {
        Student s = new Student();
        s.setGoogleId(googleId);
        s.setEmail(email);
        s.setName("Test");
        return s;
    }

    @Test
    void savesAndLooksUpByGoogleIdAndEmail() {
        studentRepository.save(student("g-1", "g1@vitapstudent.ac.in"));

        assertThat(studentRepository.findByGoogleId("g-1")).isPresent();
        assertThat(studentRepository.findByEmail("g1@vitapstudent.ac.in")).isPresent();
        assertThat(studentRepository.findByGoogleId("missing")).isEmpty();
    }

    @Test
    void enforcesUniqueGoogleId() {
        studentRepository.saveAndFlush(student("dup", "a@vitapstudent.ac.in"));

        assertThatThrownBy(() ->
                studentRepository.saveAndFlush(student("dup", "b@vitapstudent.ac.in")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
