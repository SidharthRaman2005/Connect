package com.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connect.dto.VerifiedUser;
import com.connect.entity.Student;
import com.connect.repository.StudentRepository;
import com.connect.service.StudentService.AuthResult;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private VerifiedUser verifiedUser(String email, String name, String picture) {
        return new VerifiedUser("google-sub-1", email, name, picture);
    }

    @Test
    void createsAccountOnFirstLogin() {
        when(studentRepository.findByGoogleId("google-sub-1")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = studentService.createOrAuthenticate(
                verifiedUser("new@vitapstudent.ac.in", "New Student", "pic1"));

        assertThat(result.newAccount()).isTrue();
        assertThat(result.student().getGoogleId()).isEqualTo("google-sub-1");
        assertThat(result.student().getEmail()).isEqualTo("new@vitapstudent.ac.in");
        assertThat(result.student().getName()).isEqualTo("New Student");
        assertThat(result.student().getLastLoginAt()).isNotNull();
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void authenticatesExistingAndSyncsFromGoogle() {
        Student existing = new Student();
        existing.setGoogleId("google-sub-1");
        existing.setEmail("original@vitapstudent.ac.in");
        existing.setName("Old Name");
        when(studentRepository.findByGoogleId("google-sub-1")).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = studentService.createOrAuthenticate(
                verifiedUser("ignored@vitapstudent.ac.in", "Updated Name", "pic2"));

        assertThat(result.newAccount()).isFalse();
        // Name/picture are refreshed from Google on each login...
        assertThat(result.student().getName()).isEqualTo("Updated Name");
        assertThat(result.student().getPictureUrl()).isEqualTo("pic2");
        // ...but the email is set only at creation and not overwritten.
        assertThat(result.student().getEmail()).isEqualTo("original@vitapstudent.ac.in");
    }
}
