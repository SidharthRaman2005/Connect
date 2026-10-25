package com.connect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.connect.dto.ProfileDto;
import com.connect.dto.PublicProfileDto;
import com.connect.dto.UpdateProfileRequest;
import com.connect.entity.ProfileVisibility;
import com.connect.entity.Student;
import com.connect.exception.ForbiddenException;
import com.connect.exception.ResourceNotFoundException;
import com.connect.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private ProfileService profileService;

    /** A fully populated student with the given id and visibility. */
    private Student student(Long id, ProfileVisibility visibility) {
        Student s = new Student();
        ReflectionTestUtils.setField(s, "id", id); // no setter; id is DB-generated
        s.setEmail("26mic7142@vitapstudent.ac.in");
        s.setName("Test Student");
        s.setBatch(26);
        s.setBranch("MIC");
        s.setBio("hi");
        s.setProfileVisibility(visibility);
        return s;
    }

    @Test
    void getOwnProfileReturnsFullProfileIncludingEmailAndVisibility() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student(1L, ProfileVisibility.PRIVATE)));

        ProfileDto dto = profileService.getOwnProfile(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.email()).isEqualTo("26mic7142@vitapstudent.ac.in");
        assertThat(dto.batch()).isEqualTo(26);
        assertThat(dto.visibility()).isEqualTo(ProfileVisibility.PRIVATE);
    }

    @Test
    void updateProfileTrimsBranchAppliesFieldsAndCompletesOnboarding() {
        Student existing = student(1L, ProfileVisibility.PUBLIC);
        existing.setProfileCompleted(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileDto dto = profileService.updateProfile(1L,
                new UpdateProfileRequest("  CSE  ", "my new bio", ProfileVisibility.PRIVATE));

        assertThat(dto.branch()).isEqualTo("CSE"); // trimmed
        assertThat(dto.bio()).isEqualTo("my new bio");
        assertThat(dto.visibility()).isEqualTo(ProfileVisibility.PRIVATE);
        assertThat(dto.profileCompleted()).isTrue();
    }

    @Test
    void getProfileReturnsPublicViewForPublicProfile() {
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(student(2L, ProfileVisibility.PUBLIC)));

        PublicProfileDto dto = profileService.getProfile(1L, 2L);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("Test Student");
        assertThat(dto.branch()).isEqualTo("MIC");
    }

    @Test
    void getProfileHidesPrivateProfileFromOtherStudents() {
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(student(2L, ProfileVisibility.PRIVATE)));

        assertThatThrownBy(() -> profileService.getProfile(1L, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProfileLetsOwnerViewTheirOwnPrivateProfile() {
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(student(2L, ProfileVisibility.PRIVATE)));

        PublicProfileDto dto = profileService.getProfile(2L, 2L);

        assertThat(dto.id()).isEqualTo(2L);
    }

    @Test
    void getProfileTreatsNullVisibilityAsPublicForLegacyRows() {
        when(studentRepository.findById(2L))
                .thenReturn(Optional.of(student(2L, null)));

        PublicProfileDto dto = profileService.getProfile(1L, 2L);

        assertThat(dto.id()).isEqualTo(2L);
    }

    @Test
    void getProfileThrowsWhenStudentMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
